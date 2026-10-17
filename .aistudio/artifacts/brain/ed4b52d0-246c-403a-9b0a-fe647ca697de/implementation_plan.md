# Enhanced Vertex AI Studio: Thinking Level, Multi-Theme, Background Service & Visual Polish

Enhance **Vertex AI Studio & File Client** with customizable Thinking levels, 4 distinctive color themes, background generation via an Android Foreground Service with live notifications, and upgraded visual polish.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices have been confirmed:

- **Confirmed - Thinking Level Selection**: Dual-mode controls:
  - **Preset Buttons**: `Vypnuto (0)`, `Nízká (1 024)`, `Střední (4 096)`, `Vysoká (16 384)`, `Maximum (32 768 / 65 536)`.
  - **Custom Budget Slider**: Full granular control from 0 to 65,536 thinking tokens mapped to Vertex AI `thinkingConfig.thinkingBudget`.
- **Confirmed - Color Themes**: 4 rich, hand-crafted themes:
  1. **Clean Slate** (Default, dark slate with cyan accents)
  2. **Obsidian Black** (OLED true black #000000 with electric silver/white accents)
  3. **Cyberpunk Neon** (Midnight violet #0A0612 with hot neon cyan #00F0FF & magenta #FF007F)
  4. **Forest Emerald** (Deep evergreen #05160E with vibrant jade emerald #10B981)
- **Confirmed - Background Service & Notifications**:
  - Android `GenerationForegroundService` (`foregroundServiceType="dataSync"`) keeping network streaming active when the user switches apps or turns off the screen.
  - Ongoing notification showing live stream progress (thinking stage, live code line count, generated files).
  - High-priority completion notification with alert chime when long generation finishes.

---

## 1. Overview & Feature Design

### 1. Granular Thinking Level & Token Budget
- In Settings and Chat configuration:
  - Direct toggle between presets and manual token slider.
  - Maps to Vertex AI REST body:
    ```json
    "generationConfig": {
      "thinkingConfig": {
        "thinkingBudget": 4096
      }
    }
    ```
  - When set to 0, thinking is explicitly disabled for faster responses and lower latency.
  - When enabled, thinking output stream is captured separately and rendered in the expandable `ThinkingBlock`.

### 2. Multi-Theme Engine (`AppThemeMode`)
- `Theme.kt` and `Color.kt` expanded to support:
  - `SLATE`, `OBSIDIAN`, `CYBERPUNK`, `EMERALD`.
  - Persistent in `SettingsDataStore`.
  - Full Material 3 colorScheme palette for each theme, respecting light and dark mode preferences.

### 3. Background Generation (`GenerationForegroundService`)
- Android 14+ compatible `ForegroundService` with notification channel:
  - Channel: `"vertex_ai_streaming"` (High importance).
  - Notification icon, title `"Vertex AI generuje..."`, text showing real-time token/file stats.
  - Action button: `"Zastavit"` to cancel generation from notification bar.
  - Runtime permission handler in Compose for `POST_NOTIFICATIONS` (Android 13+).

### 4. Elevated Visual Polish & Aesthetics
- Glassmorphic card styling with glowing accent borders.
- Live streaming HUD: real-time token rate badge, animated pulse indicator during thinking vs writing code.
- Refined typography and spacious layout with subtle depth elevation.

---

## 2. Technical Architecture & Component Flow

```
┌────────────────────────────────────────────────────────┐
│                        UI LAYER                        │
│   ChatScreen ──> Live HUD (Thinking/Writing Status)    │
│   SettingsScreen ──> Thinking Presets + Theme Picker   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                   VIEWMODEL & SERVICE                  │
│   ChatViewModel                                        │
│     ├── Triggers GenerationForegroundService           │
│     └── Updates Live Notification with Stream State    │
│                                                        │
│   GenerationForegroundService                          │
│     ├── Foreground Service Notification (Progress)     │
│     └── Completion & Stop Broadcast Intent Handlers    │
└───────────────────────────┬────────────────────────────┘
                            │
       ┌────────────────────┴────────────────────┐
       ▼                                         ▼
┌─────────────────────────────┐   ┌─────────────────────────────┐
│      SettingsDataStore      │   │     VertexAiApiClient       │
│  - appTheme: String         │   │  - thinkingBudget: Int      │
│  - thinkingBudget: Int      │   │  - streamGenerateContent    │
│  - thinkingPreset: String   │   │                            │
└─────────────────────────────┘   └─────────────────────────────┘
```

---

## 3. Execution Plan
1. Update `AndroidManifest.xml` with `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS` permissions and register `GenerationForegroundService`.
2. Expand `data/local/datastore/SettingsDataStore.kt` with `appTheme`, `thinkingBudget`, and `thinkingPreset`.
3. Update `data/remote/dto/VertexModels.kt` and `VertexAiApiClient.kt` to include `thinkingConfig.thinkingBudget` in JSON requests.
4. Expand `ui/theme/Color.kt` and `ui/theme/Theme.kt` with the 4 themes (Slate, Obsidian, Cyberpunk, Emerald).
5. Implement `service/GenerationForegroundService.kt` for background execution and notifications.
6. Enhance `ui/chat/ChatViewModel.kt` to coordinate with the foreground service and update notification state.
7. Update `ui/chat/components/MessageItem.kt`, `ui/chat/ChatScreen.kt`, and `ui/settings/SettingsScreen.kt` with visual polish, theme selector, and thinking controls.
8. Verify and validate compilation via `compile_applet`.
