# Implementační plán: Elegantní jednořádkový Thinking blok & Vizuální dokonalost aplikace (DOKONČENO ✅)

Implementace kompaktního, vysoce elegantního zobrazení uvažování (thinking) modelu sbaleného do jednoho řádku s ikonou mozku a počtem slov, spolu s celkovým vizuálním auditem, odstraněním chyb a zdokonalením UI v celé aplikaci.

---

## 1. Shrnutí provedených úprav & uživatelských požadavků

1. **Výchozí stav bloku uvažování (ThinkingBlock)**:
   - ✅ Vždy výchozivě sbalený do jediného čistého elegantního řádku.
   - ✅ Obsahuje ikonu mozku (`Icons.Default.Psychology`) v zaobleném kruhovém kontejneru.
   - ✅ Zobrazuje text *"Myšlenkový proces"*.
   - ✅ Obsahuje zaoblený pill odznak s přesným dynamickým počtem slov (např. *"248 slov"*).
   - ✅ Obsahuje animovanou otočnou šipku pro rozbalení/sbalení s textovým hintem *"Rozbalit"* / *"Sbalit"*.
   - ✅ Při rozbalení plynulou animací (`expandVertically + fadeIn`) zobrazí interní tok uvažování v přehledném monospace formátování spolu s tlačítkem *"Kopírovat"*.

2. **Oprava zadávání vlastního argumentu (Custom Argument Key)**:
   - ✅ Textové pole pro zadání libovolného vlastního argumentu (např. `premysleni_pro_api`, `my_custom_param`, ...) je umístěno na prvním a nejviditelnějším místě ve všech dialozích (`ThinkingLevelManagerDialog`, `ModelManagerDialog`, `SettingsScreen`, `ChatScreen`).
   - ✅ Odstraněn matoucí/chybný přednastavený čip `premysleni_pro_api`, který působil jako zabugovaná volba namísto volného textového pole.
   - ✅ Pod polem jsou přehledně umístěny pouze standardní pomocné čipy pro Vertex AI / Gemini 2.5 (`thinkingLevel`, `thinking_level`, `thinkingBudget`).

3. **Vizuální opravy a doladění rozvržení v aplikaci**:
   - ✅ Opraveny všechny přetékající a oříznuté čipy pomocí `FlowRow`.
   - ✅ Všechny formuláře a editační dialogy mají `verticalScroll` pro plynulý průchod i při otevřené klávesnici.
   - ✅ Konzistentní zaoblení, barevný kontrast v temném režimu a čisté odsazení v bublinách zpráv.

4. **Verifikace a testování**:
   - ✅ `compile_applet` úspěšně zkompilován bez varování a chyb.
   - ✅ Robolectric testy (`ThinkingConfigurationTest`) ověřují serializaci JSON payloadu s vlastním klíčem argumentu i výpočet počtu slov uvažování (BUILD SUCCESSFUL).
