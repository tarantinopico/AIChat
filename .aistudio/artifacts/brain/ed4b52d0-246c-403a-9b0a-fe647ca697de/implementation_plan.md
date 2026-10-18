# Neomezené opakování v artefaktu & Oprava / Rozšíření maximálního kontextu

Tento plán řeší dvě klíčové oblasti: zpřístupnění volby **Neomezeně (∞)** pro automatické navazování kódu v artefaktu a zásadní rozšíření/opravu **maximálního kontextu konverzace** (posuvník až do 500 zpráv + režim celé historie bez ořezávání).

---

## Uživatelský přehled & Potvrzená rozhodnutí

> [!IMPORTANT]
> **Klíčová rozhodnutí na základě vašich odpovědí:**
> 1. **Neomezené automatické navazování (Auto-Continue)**:
>    - K existujícím volbám (1×, 2×, 3×, 5×) přibude volba **`∞ Neomezeně`**.
>    - Model bude automaticky pokračovat v dopisování souboru v artefaktu tak dlouho, dokud kód kompletně nedokončí (nebo dokud uživatel nestiskne tlačítko *Zastavit*).
> 2. **Oprava a zásadní zvětšení kontextového limitu**:
>    - Stávající posuvník (který měl strop pouhých 50 zpráv) bude rozšířen až do **500 zpráv** s rychlými čipy (20, 50, 100, 250, 500).
>    - Navíc přibude přímý přepínač **„Neomezený kontext (celá historie konverzace)“**, který do Vertex AI pošle kompletní historii bez jakéhokoliv ořezávání (využívá obří 1M–2M tokenové okno Gemini 2.5 Pro).

---

## 1. Architektura & Tok dat

```
┌────────────────────────────────────────────────────────────────────────┐
│                          UŽIVATELSKÉ ROZHRANÍ                         │
│  ┌──────────────────────────────────┐  ┌────────────────────────────┐  │
│  │ SettingsScreen                   │  │ ChatScreen / HUD           │  │
│  │ - Auto-Continue: [1×, 3×, 5×, ∞] │  │ - Běžící navazování (#3)   │  │
│  │ - Kontext: [Celá historie / 500] │  │ - Stop tlačítko            │  │
│  └──────────────────┬───────────────┘  └─────────────┬──────────────┘  │
└─────────────────────┼────────────────────────────────┼─────────────────┘
                      │                                │
                      ▼                                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        ChatViewModel & DataStore                       │
│  - maxAutoContinueSteps = 0 (reprezentuje ∞ Neomezeně)                 │
│  - isUnlimitedContext = true OR contextLimit = 5..500                  │
│  - Sestavení kontextu: celá historie vlákna se všemi artefakty        │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      Vertex AI Gemini API Klient                       │
│  - Odesílá kompletní kontext bez předčasného ořezávání                │
│  - Automatické navazování běží do uzavření kódu či ručního stopu       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Uživatelská zkušenost & Vizuální design (UX/UI)

1. **Sekce Nastavení parametrů (`SettingsScreen`)**:
   - **Karta Kontextového limitu**:
     - Hlavní přepínač: *„Neomezený kontext (všechny zprávy vlákna)“*
     - Vysvětlivka: *„Vertex AI modely Gemini 1.5 a 2.5 podporují kontext 1 až 2 miliony tokenů. V tomto režimu model nezapomene žádný kód ani zprávu z celého vlákna.“*
     - Pokud je přepínač vypnutý: plynulý posuvník od 5 do 500 zpráv s rychlými čipy: `[20] [50] [100] [250] [500]`.
     - Živý indikátor aktuálního nastavení: *„Aktivní limit: 500 zpráv v kontextu“* resp. *„Aktivní: Kompletní historie vlákna (neomezeně)“*.

2. **Sekce Automatického pokračování (`SettingsScreen`)**:
   - Vylepšená lišta s čipy pro počet navázání:
     `[1×] [2×] [3×] [5×] [∞ Neomezeně]`
   - Výrazné vizuální potvrzení stavu:
     *„⚡ Auto-Continue: Model bude navazovat v artefaktu bez omezení až do úplného dokončení kódu.“*

3. **Indikace v chatu během neomezeného navazování**:
   - Informační badge a HUD: *„Auto-Continue: Navázáno 4× (režim Neomezeně)...“*
   - Vždy dostupné výrazné červené tlačítko **„Zastavit“** v horní stavové liště i v trvalé notifikaci na pozadí pro případ, že uživatel chce generování kdykoliv ukončit.

---

## 3. Technická realizace krok za krokem

### Krok 1: Rozšíření `SettingsDataStore.kt`
- Přidání preference `val IS_UNLIMITED_CONTEXT = booleanPreferencesKey("is_unlimited_context")`.
- Úprava `AppSettings`:
  - `isUnlimitedContext: Boolean = false` (nebo `true` dle volby)
  - `contextLimit: Int = 100` (výchozí navýšeno z 20 na 100, rozsah 5 až 500)
  - `maxAutoContinueSteps: Int`: hodnota `0` reprezentuje `∞ Neomezeně`.
- Doplnění metod `updateContextSettings(isUnlimited: Boolean, limit: Int)`.

### Krok 2: Úprava logiky sestavení kontextu v `ChatViewModel.kt`
- V `triggerModelGeneration`:
  - Pokud je `currentSettings.isUnlimitedContext == true`:
    `messagesToConsider = allDbMessages` (žádné `takeLast` ořezávání!).
  - Pokud je `isUnlimitedContext == false`:
    `recentNonPinned = nonPinnedMessages.takeLast(currentSettings.contextLimit)`.
- V logice zřetězeného navazování:
  - Pokud `currentSettings.maxAutoContinueSteps == 0`:
    Povoleno neomezené navazování bez horního stropu `updatedCount < maxAutoContinueSteps`, dokud `lastFinishReason == "MAX_TOKENS"`.

### Krok 3: Modernizace UI v `SettingsScreen.kt`
- Implementace přepínače neomezeného kontextu a posuvníku s rozsahem `5..500`.
- Doplnění volby `∞ Neomezeně` do čipů Auto-Continue s odpovídající barevnou signalizací.

---

## 4. Ověření a testy

1. **Unit testy v `StreamFileExtractorContinuationTest` & nové testy kontextu**:
   - Ověření, že při `maxAutoContinueSteps == 0` proces pokračuje.
   - Ověření, že při `isUnlimitedContext == true` jsou do kontextu zahrnuty všechny zprávy.
2. **Kompilace aplikace**:
   - Spuštění `compile_applet` a `gradle testDebugUnitTest` k ověření 100% stability.
