# GitHub Actions CI/CD Workflow: APK Build & Release

An automated GitHub Actions workflow to build, package, and deliver debug APKs for **Vertex AI Studio & File Client** on every push, pull request, manual dispatch, and git release tag.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following workflow configuration decisions have been confirmed:

- **Confirmed - Build Variant**: **Debug APK** (`:app:assembleDebug`). Allows direct download and sideloading on any Android phone or emulator without requiring complex keystore setup in GitHub Secrets.
- **Confirmed - Trigger Conditions**: Runs automatically on:
  - `push` to the `main` branch
  - `pull_request` targeting `main`
  - `push` of version tags (e.g., `v*`)
  - `workflow_dispatch` (manual trigger button in GitHub UI with optional inputs)
- **Confirmed - Delivery & Distribution**:
  - Uploads the compiled `.apk` as a downloadable GitHub Actions artifact (`vertex-ai-studio-debug-apk`).
  - When triggered by a git tag (e.g., `git push origin v1.0.0`), automatically creates a GitHub Release and attaches the `.apk` file for direct public downloading.

---

## 1. Overview & Workflow Design

The workflow `.github/workflows/build-apk.yml` handles the entire Android compilation pipeline on GitHub-hosted runners (`ubuntu-latest`).

### Pipeline Stages
1. **Repository Checkout**: Clones the repository with full git history.
2. **Environment & Secrets Initialization**: Creates a valid `.env` file from `.env.example` so the Secrets Gradle Plugin resolves smoothly during the build.
3. **Java & Android SDK Setup**: Configures JDK 17 (Temurin) and ensures Android build-tools and platforms are present.
4. **Gradle & Wrapper Provisioning**: Ensures Gradle wrapper is initialized and cached via `gradle/actions/setup-gradle@v4`.
5. **Compilation (`assembleDebug`)**: Builds `:app:assembleDebug`, producing `app-debug.apk`.
6. **Artifact Storage**: Uploads the APK to GitHub Actions run artifacts for 14-day retention.
7. **Automated Release on Tag**: Automatically publishes a GitHub Release with release notes and attaches the APK whenever a version tag is pushed.

---

## 2. Technical Architecture & CI/CD Flow

```
┌────────────────────────────────────────────────────────┐
│                   GitHub Triggers                      │
│   • push: main         • pull_request: main            │
│   • tags: 'v*'         • workflow_dispatch (Manual)    │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                Runner: ubuntu-latest                   │
│  1. actions/checkout@v4                                │
│  2. actions/setup-java@v4 (Java 17 Temurin)            │
│  3. gradle/actions/setup-gradle@v4 (Caching & Setup)   │
│  4. Generate dummy .env (Secrets plugin compatibility) │
│  5. Run build: ./gradlew :app:assembleDebug            │
└───────────────────────────┬────────────────────────────┘
                            │
       ┌────────────────────┴────────────────────┐
       ▼                                         ▼
┌─────────────────────────────┐   ┌─────────────────────────────┐
│    Always (Push / PR / UI)  │   │     If Tag Push ('v*')      │
│ actions/upload-artifact@v4  │   │ softprops/action-gh-release │
│  - vertex-ai-studio-apk     │   │  - Creates GitHub Release   │
│  - Retention: 14 days       │   │  - Attaches app-debug.apk   │
└─────────────────────────────┘   └─────────────────────────────┘
```

---

## 3. Key Product & Security Decisions

- **Handling `.env` Secrets**: Android project uses `com.google.android.libraries.mapsplatform.secrets-gradle-plugin`. In CI without configured secrets, a missing `.env` causes build failure. The workflow automatically creates a minimal `.env` file (`touch .env`) to guarantee zero-configuration compilation.
- **Gradle Caching**: Uses official `gradle/actions/setup-gradle@v4` to cache dependencies and build cache, reducing build times from minutes to seconds on subsequent runs.
- **Executable Permissions**: Ensures `chmod +x gradlew` is applied before invocation.
- **Git Wrapper Generation**: Generates `gradlew` script and commits it alongside the workflow so any developer or CI runner can invoke `./gradlew` immediately without requiring a pre-installed Gradle binary.

---

## 4. Execution Steps
1. Generate standard executable `gradlew` and `gradlew.bat` files matching Gradle 9.3.1.
2. Create `.github/workflows/build-apk.yml` with all triggers, build steps, artifact upload, and release automation.
3. Validate and verify project compilation.
