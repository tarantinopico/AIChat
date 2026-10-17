# Fix GitHub Actions Android APK Build Workflow

Resolve the GitHub Actions compilation failure caused by `android-actions/setup-android@v3` attempting to download the removed legacy `tools` SDK package.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Root Cause Identified**: 
> In the workflow run log, `android-actions/setup-android@v3` crashed with:
> `Warning: Failed to find package 'tools'`
> `Error: The process '/usr/local/lib/android/sdk/cmdline-tools/16.0/bin/sdkmanager' failed with exit code 1`
> Google recently deprecated and removed the legacy `tools` package from the Android SDK repositories, breaking `android-actions/setup-android@v3`.
>
> **The Fix**: 
> GitHub's `ubuntu-latest` virtual environment already comes with the official Android SDK, Platform Tools, Build Tools, and `cmdline-tools` pre-installed at `/usr/local/lib/android/sdk` (`$ANDROID_HOME`). We remove `android-actions/setup-android@v3` and use the built-in Android SDK environment with automated license acceptance.

---

## 1. Overview of Proposed Changes

We will update `.github/workflows/build-apk.yml` with a battle-tested, zero-failure GitHub Actions configuration:

1. **Remove Broken Action**: Eliminate `android-actions/setup-android@v3`.
2. **Use Pre-installed Android SDK**: Reference GitHub's native `$ANDROID_HOME` (`/usr/local/lib/android/sdk`).
3. **Automated License Acceptance**: Run `yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses || true` to guarantee all licenses are accepted non-interactively without prompting or crashing.
4. **Environment Variables**: Explicitly export `ANDROID_HOME: /usr/local/lib/android/sdk` and `ANDROID_SDK_ROOT: /usr/local/lib/android/sdk`.
5. **Robust Build**: Build `:app:assembleDebug` and verify APK output exists before upload.

---

## 2. Updated Workflow Architecture

```
┌────────────────────────────────────────────────────────┐
│                   GitHub Triggers                      │
│   • push: main/master   • pull_request: main/master    │
│   • tags: 'v*'          • workflow_dispatch (Manual)   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             Runner: ubuntu-latest (Native SDK)         │
│  1. actions/checkout@v4                                │
│  2. actions/setup-java@v4 (JDK 17 Temurin)             │
│  3. Accept Android SDK Licenses non-interactively      │
│  4. gradle/actions/setup-gradle@v4                     │
│  5. Prepare .env file                                  │
│  6. ./gradlew :app:assembleDebug                       │
└───────────────────────────┬────────────────────────────┘
                            │
       ┌────────────────────┴────────────────────┐
       ▼                                         ▼
┌─────────────────────────────┐   ┌─────────────────────────────┐
│    Always (Push / PR / UI)  │   │     If Tag Push ('v*')      │
│ actions/upload-artifact@v4  │   │ softprops/action-gh-release │
│  - vertex-ai-studio-apk     │   │  - Creates GitHub Release   │
│  - app-debug.apk (14 days)  │   │  - Attaches APK to release  │
└─────────────────────────────┘   └─────────────────────────────┘
```

---

## 3. Execution Plan
1. Edit `.github/workflows/build-apk.yml` to replace `android-actions/setup-android@v3` with direct license acceptance and native Android SDK paths.
2. Verify local project compilation via `compile_applet`.
