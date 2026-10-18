# Oprava pokračování v artefaktu, Automatické navazování v nastavení & Běh na pozadí

Komplexní řešení pro bezchybné pokračování generování kódu přímo v existujícím artefaktu (bez fragmentace zpráv a souborů), plné zpřístupnění a zdokonalení automatického pokračování v nastavení a zajištění nepřerušeného běhu na pozadí prostřednictvím Android Foreground Service.

---

## Uživatelský přehled & Klíčová rozhodnutí

> [!IMPORTANT]
> **Klíčové principy navrženého řešení:**
> 1. **Jednolitý artefakt (Seamless Artifact Continuation)**: Při dosažení limitu `MAX_TOKENS` a spuštění pokračování (manuálně i automaticky) se nový text a kód plynule napojí do **stávajícího souboru v artefaktu** a původní zprávy, namísto vytváření rozbitých oddělených zpráv bez syntaxe.
> 2. **Tlačítko pokračování přímo v prohlížeči artefaktu**: Uživatel může pokračovat nejen z bubliny v chatu, ale také přímo z dialogu prohlížeče souboru (`FileArtifactViewerDialog`).
> 3. **Přehledné nastavení automatického pokračování**: V `SettingsScreen` bude velký jasný přepínač pro *Automatické pokračování při limitu tokenů* s volbou maximálního počtu navázání (1–5×), aby nedošlo k nekonečné smyčce.
> 4. **Spolehlivý běh na pozadí**: Generování a navazování probíhá přes aplikační `CoroutineScope` a `GenerationForegroundService`, takže minimalizace aplikace, zhasnutí obrazovky nebo přepnutí do jiné aplikace proces nepřeruší.

---

## 1. Přehled & Architektura řešení

```
┌────────────────────────────────────────────────────────────────────────┐
│                          UŽIVATELSKÉ ROZHRANÍ                         │
│  ┌───────────────────────┐  ┌──────────────────────┐  ┌─────────────┐  │
│  │ ChatScreen / Bubble   │  │ FileArtifactViewer   │  │ Settings    │  │
│  │ [Pokračovat v psaní]  │  │ [Pokračovat v kódu]  │  │ Auto-cont.  │  │
│  └───────────┬───────────┘  └──────────┬───────────┘  └──────┬──────┘  │
└──────────────┼─────────────────────────┼─────────────────────┼─────────┘
               │                         │                     │
               ▼                         ▼                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         ChatViewModel & Repository                     │
│  - continuesInTargetMessage(targetMessageId, targetFileId)             │
│  - čte Settings: autoContinueEnabled, maxContinuations                 │
│  - řídí počet navázání (continuationCount <= maxContinuations)         │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │                                 │
                   ▼                                 ▼
┌───────────────────────────────────┐  ┌─────────────────────────────────┐
│   GenerationForegroundService     │  │        StreamFileExtractor      │
│  - NOTIFICATION s reálným stavem │  │  - Inteligentní slučování       │
│  - Zajišťuje WAKE / běh na pozadí │  │    neuzavřených code-blocků     │
│  - Notifikace o dokončení         │  │  - Plynulý append do souboru    │
└───────────────────────────────────┘  └─────────────────────────────────┘
```

---

## 2. Uživatelská zkušenost & Vizuální design (UX/UI)

1. **Plynulé navázání v bublině zprávy**:
   - Pokud model narazí na `MAX_TOKENS`, zobrazí se elegantní informační lišta: *"Kód byl zastaven z důvodu limitu tokenů"*.
   - Vedle je výrazné tlačítko *"Pokračovat v kódu"* se šipkou a ikonou souboru.
   - Po kliknutí se nespustí tvorba nové zprávy, nýbrž se přímo v této zprávě a v jejím souboru začne v reálném čase streamovat doplňovaný kód.
   - U zprávy se zobrazí jemný štítek: *"Pokračováno (1×)"*.

2. **Prohlížeč artefaktu (`FileArtifactViewerDialog`)**:
   - Pokud je artefakt neúplný (`isComplete == false`), na spodní liště dialogu se vedle tlačítek *"Kopírovat"* a *"Zavřít"* zobrazí prominentní tlačítko:
     `[▶ Pokračovat v generování souboru]`.
   - Během generování uživatel přímo v prohlížeči vidí, jak přibývají nové řádky kódu.

3. **Nastavení automatického pokračování (`SettingsScreen`)**:
   - Nová přehledná karta v sekci *Přepínače chování*:
     - **Hlavní přepínač**: *Automaticky navazovat neúplný kód (Auto-Continue)*
     - **Vysvětlivka**: *"Při vyčerpání maximálního počtu tokenů automaticky vyzve model k pokračování v psaní otevřeného souboru."*
     - **Volba limitu opakování**: Rychlé čipy pro max počet automatických navázání: `1×`, `2×`, `3×`, `5×`.

4. **Běh a notifikace na pozadí**:
   - Probíhající generování a pokračování zobrazuje průběžnou notifikaci v systému Android (Foreground Service).
   - Text notifikace: *"Generuji soubor: MainActivity.kt (řádek 145...)"*.
   - Po dokončení se zobrazí notifikace *"Kód byl úspěšně vygenerován (celkem 320 řádků)"*.

---

## 3. Technická realizace krok za krokem

### Krok 1: Logika slučování v `StreamFileExtractor` a `ChatViewModel`
- Přidání metody `mergeContinuation(originalText, originalFiles, newChunkText)`:
  - Detekuje, zda byl původní kód zakončen otevřeným blokem ` ```<lang> `.
  - Pokud model v pokračování vynechal úvodní ` ``` `, kód je přímo připojen do těla posledního souboru.
  - Pokud model zahájil novým ` ``` `, bloky jsou korektně spojeny do jednoho celistvého souboru.
- V `ChatViewModel.kt`:
  - `continueGeneration(messageId, targetFileId = null)`:
    - Namísto vkládání nové zprávy `chatDao.insertMessage` aktualizuje původní `ChatMessageEntity` (`chatDao.updateMessage`) a příslušný `FileArtifactEntity` (`chatDao.updateFile`).
    - Ukládá přesný `continuePrompt` formátovaný tak, aby model okamžitě navázal bez zbytečného úvodního povídání.

### Krok 2: Rozšíření `SettingsDataStore` a `SettingsScreen`
- Doplnění vlastností do `SettingsDataStore`:
  - `autoContinueEnabled: Boolean` (výchozí `true` nebo dle uživatele)
  - `maxAutoContinueSteps: Int` (výchozí `3`)
- Vytvoření moderního UI bloku v `SettingsScreen.kt` s přepínačem a čipy počtu kroků.

### Krok 3: Spolehlivý Foreground Service pro běh na pozadí
- Zajištění, že `activeGenerationJob` není vázán pouze na dočasný životní cyklus obrazovky, ale běží pod aplikačním kontextem `ApplicationScope` a `GenerationForegroundService`.
- Služba správně drží `PARTIAL_WAKE_LOCK` během streamování přes síť, aby systém neuspal CPU při zhasnutí displeje.

### Krok 4: Tlačítko v `FileArtifactViewerDialog`
- Přidání callbacku `onContinueGeneration: (Long) -> Unit` do dialogu prohlížeče souboru.
- Tlačítko pro okamžité navázání s animovaným indikátorem stavu.

---

## 4. Ověření a testování

1. **Unit & Robolectric testy**:
   - Ověření slučování useknutého kódu v `StreamFileExtractorTest`:
     - Test: Neuzavřený blok kódu + pokračování bez ` ``` ` -> jeden validní spojený soubor.
     - Test: Neuzavřený blok kódu + pokračování s ` ``` ` -> jeden validní spojený soubor.
2. **Kompilace aplikace**:
   - Spuštění `compile_applet` pro ověření 100% stability buildu bez chyb.
