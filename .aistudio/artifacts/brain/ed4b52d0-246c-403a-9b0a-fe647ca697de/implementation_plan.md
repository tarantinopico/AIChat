# Model Manager (CRUD), Thinking Auto-Detection & Permanent API Error Resolution

A comprehensive system upgrade providing full CRUD management for Vertex AI models, automatic capability detection with manual toggles, complete control over thinking feature visibility, and permanent elimination of the `thinking_level is not supported by this model` error.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Summary of Confirmed Decisions from Phase 1**:
> 1. **Thinking Level Configuration**: Automatic model capability detection (e.g. Gemini 2.5 supports thinking, Gemini 1.5/2.0-flash does not) with per-model manual override toggle.
> 2. **Model Management**: Full Model Manager interface with Create, Read, Update, Delete (CRUD) and direct per-model "Test Spojení" (ping) connectivity test.
> 3. **Error Recovery**: Display an actionable error card with a one-tap `[⚡ Spustit znovu bez uvažování]` button that automatically marks the model as non-thinking and resumes generation instantly.
> 4. **Thinking Visibility Control**: Added a master toggle in Settings to completely hide/disable the thinking level selector across the entire app if desired.

---

## 1. Overview & Core Concept

Vertex AI rejects `thinkingConfig` (and `thinking_level`) when sent to models that do not support reasoning (such as `gemini-1.5-pro`, `gemini-1.5-flash`, `gemini-2.0-flash`). This upgrade delivers:
- **Intelligent Thinking Gate**: Only models configured with thinking capability will attach `thinkingConfig`. For non-thinking models, `thinkingConfig` is completely omitted.
- **Actionable Chat Error Card**: If any model unexpectedly rejects thinking, an actionable error banner appears directly on the message with a one-tap `[⚡ Zkusit znovu bez uvažování]` button, which instantly updates the model's profile and re-runs generation.
- **Full Model Manager (CRUD)**:
  - Add custom Vertex AI models (e.g. fine-tuned endpoints, experimental checkpoints).
  - Edit model metadata (display name, max output tokens, temperature, thinking support toggle).
  - Delete user-created models.
  - Test model connectivity individually with real-time ping.
- **Full UI Polish & Empty States**:
  - Empty conversation states with fast-start prompt cards.
  - Empty model list states with guidance.
  - Sleek visual badges showing whether a model supports reasoning.

---

## 2. User Experience & Visual Design

### 2.1 Chat Screen & TopAppBar Experience
- **Thinking Chip Behavior**:
  - If the active model does **not** support thinking: The chip displays `[🧠 Není podporováno]` (or is hidden if global thinking toggle is disabled in Settings). Clicking it explains that this model operates in standard direct mode.
  - If the model **does** support thinking: The chip shows `[🧠 Off / Low / High ▾]` with instant switching.
- **Actionable Error Card**:
  - When an API error occurs regarding thinking support, the error message highlights the issue and presents a prominent primary button:
    `[⚡ Zkusit znovu bez uvažování (Fix & Retry)]`.
  - Tapping it immediately disables thinking for that model and re-triggers generation without losing prompt context.
- **Empty State**:
  - Welcoming hero header with quick starter chips:
    - ⚡ *Generovat Android Service*
    - 📁 *Vytvořit souborový parser*
    - 🛠️ *Navrhnout Compose UI rozhraní*

### 2.2 Model Manager (CRUD Dialog / Screen)
- Accessible from both the Chat TopAppBar model dropdown (`[⚙️ Spravovat modely...]`) and Settings.
- **Model List**:
  - Categorized into "Google Cloud Vertex Modely" and "Vlastní Modely".
  - Each item shows: Model ID, Display Name, status badges (`[🧠 Thinking]` or `[⚡ Standard]`), and quick action buttons (Test, Edit, Delete).
- **Model Editor Modal**:
  - Model ID (e.g. `gemini-2.5-pro`, `projects/.../endpoints/...`)
  - Friendly Name
  - Checkbox: "Podporuje Thinking (Uvažování)"
  - Max Output Tokens & Default Temperature
  - Test ping button with live response check

### 2.3 Settings Screen Master Toggles
- New toggle: **"Zobrazovat výběr Thinking v aplikaci"** (Master Thinking Switch).
  - When disabled, disables all thinking payloads and completely hides the chip in Chat.
- Dedicated section linking directly to the Full Model Manager.

---

## 3. Key Technical Decisions & Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       ChatScreen                            │
│  - TopAppBar (Model Picker + [⚙️ Model Manager] + [🧠 Chip]) │
│  - Messages LazyColumn with Actionable Error Card           │
│  - Empty State Quick Starters                               │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      ChatViewModel                          │
│  - Model configs state & persistence                        │
│  - Active model capability checking                         │
│  - One-tap error retry without thinking                     │
│  - Model CRUD operations (Add, Edit, Delete, Test)          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│     SettingsDataStore        │ │     VertexAiApiClient        │
│  - ModelConfig list (JSON)   │ │  - Request builder with      │
│  - enableThinkingFeature     │ │    strict capability check   │
│  - Active model ID           │ │  - Robust error parser       │
└──────────────────────────────┘ └──────────────────────────────┘
```

### 3.1 ModelConfig Data Structure
```kotlin
data class ModelConfig(
    val id: String,
    val name: String,
    val supportsThinking: Boolean = false,
    val defaultThinkingLevel: String = "HIGH",
    val maxOutputTokens: Int = 8192,
    val isCustom: Boolean = false
)
```

### 3.2 Safe Payload Generation in `VertexModels.kt`
- Generation config only attaches `thinkingConfig` when:
  `supportsThinking == true && thinkingLevel != "OFF" && enableThinkingFeature == true`
- If any condition is false, `thinkingConfig` is completely omitted from the payload.

---

## 4. Execution Steps

1. **Model Representation (`ModelConfig.kt`)**:
   - Define `ModelConfig` model with default library of official models (Gemini 2.5 Pro marked with thinking; 1.5 Pro, 1.5 Flash, 2.0 Flash marked without thinking).
2. **DataStore Settings Extension**:
   - Add `enableThinkingFeature` boolean key (default: true).
   - Add `modelConfigs` JSON preference with CRUD helper methods (add, update, delete, getById).
3. **Payload Fix (`VertexModels.kt` & `VertexAiApiClient.kt`)**:
   - Check model `supportsThinking` before including `thinkingConfig`.
   - Add specialized error classification for thinking incompatibility.
4. **Actionable Error Card in Message UI**:
   - In `MessageItem.kt`, if an error is identified as thinking rejection, render an actionable card with `Zkusit znovu bez uvažování`.
5. **Model Manager Dialog (`ModelManagerDialog.kt`)**:
   - Build a Material 3 dialog with listing, filter search, add new model, edit model modal, test connectivity, and delete.
6. **Chat Screen & Settings Updates**:
   - Hook Model Manager into Chat TopAppBar and Settings.
   - Add Empty State in Chat when no messages exist.
7. **Compilation & Verification**:
   - Run `compile_applet` to ensure pristine compilation and error-free execution.
