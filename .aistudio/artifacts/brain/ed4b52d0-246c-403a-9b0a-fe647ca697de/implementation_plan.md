# Fix Thinking Level API Error & In-Chat Thinking Selector

Fix the Vertex AI `HTTP 400: Thinking budget is not supported for this model. Use thinking level instead.` error and add a one-tap Thinking switcher directly to the Chat screen.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Root Cause Identified**:
> Google Gemini 2.5 / 3 models on Vertex AI reject `thinkingBudget` (integer) and require `thinkingLevel` (`"LOW"`, `"HIGH"`, or `"OFF"` / omitted). Sending `thinkingBudget` triggers:
> `HTTP 400: Thinking budget is not supported for this model. Use thinking level instead.`
>
> **The Solution**:
> 1. **Thinking API Fix**:
>    - Map thinking modes to `thinkingLevel`:
>      - `OFF`: Omit `thinkingConfig` (or set `thinkingLevel: "OFF"`), providing maximum speed and compatibility.
>      - `LOW`: `thinkingConfig: { "thinkingLevel": "LOW" }` (concise thinking).
>      - `HIGH` / `MAX` / `MEDIUM`: `thinkingConfig: { "thinkingLevel": "HIGH" }` (deep reasoning).
>    - Include automatic fallback in `VertexAiApiClient`: if a model expects `thinkingBudget` or `thinkingLevel`, the client handles it transparently without failing.
> 2. **In-Chat Thinking Selector**:
>    - Add an interactive Thinking Chip in the Chat TopAppBar right beside the model dropdown:
>      `[🧠 Off]`, `[🧠 Low]`, or `[🧠 High]`.
>    - Clicking the chip opens a quick dropdown menu allowing instant switching anytime mid-conversation without having to open the Settings screen!

---

## 1. Overview & UI Changes

### In-Chat Thinking Quick Selector
- Located directly in `ChatScreen`'s `TopAppBar`:
  - Visual status pill: shows brain icon + current thinking mode (`Off`, `Low`, `High`).
  - Dropdown options:
    - ❌ **Vypnuto (Off)**: Rychlá odezva bez uvažování
    - ⚡ **Nízká (Low)**: Rychlé lehké přemýšlení
    - 🧠 **Vysoká (High)**: Hloubkové uvažování pro komplexní kód
  - Changes are applied instantly to `ChatViewModel` and active chat generation.

### Vertex AI Request Body Fix
- Update `VertexGenerationConfig`:
  ```json
  "generationConfig": {
    "temperature": 0.7,
    "topP": 0.95,
    "topK": 40,
    "maxOutputTokens": 8192,
    "thinkingConfig": {
      "thinkingLevel": "LOW" // or "HIGH"
    }
  }
  ```
- When thinking is `OFF` (or budget 0), `thinkingConfig` is completely omitted from the payload, preventing any invalid argument errors on standard models.

---

## 2. Execution Plan
1. Update `data/remote/dto/VertexModels.kt` to serialize `thinkingConfig` with `thinkingLevel` ("LOW", "HIGH") or omit when disabled.
2. Update `data/local/datastore/SettingsDataStore.kt` with `thinkingLevel` ("OFF", "LOW", "HIGH") and sync functions.
3. Update `ui/chat/ChatViewModel.kt` to expose quick thinking toggling (`setThinkingLevel("OFF" | "LOW" | "HIGH")`).
4. Update `ui/chat/ChatScreen.kt` to add the interactive Thinking chip and dropdown in the `TopAppBar`.
5. Update `ui/settings/SettingsScreen.kt` to align with `thinkingLevel`.
6. Verify and compile via `compile_applet`.
