# Fix GitHub Actions Keystore & Build Validation

Resolve the GitHub Actions build error where `debug.keystore` was not found during `:app:validateSigningDebug`.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Root Cause Identified**:
> In the GitHub Actions run log:
> `Execution failed for task ':app:validateSigningDebug'.`
> `> Keystore file '/home/runner/work/AIChat/AIChat/debug.keystore' not found for signing config 'debugConfig'.`
> 
> In `.gitignore`, `debug.keystore` is ignored so it does not get checked into git. AI Studio's local container automatically provisions `debug.keystore` before compiling, but GitHub Actions runners clone a clean repository without it.
>
> **The Fix**:
> 1. Add an automated keystore preparation step in `.github/workflows/build-apk.yml`:
>    - If `debug.keystore.base64` is present in the repository, decode it: `base64 -d debug.keystore.base64 > debug.keystore`
>    - Otherwise, automatically generate a valid `debug.keystore` using standard `keytool`:
>      `keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"`
> 2. Fix deprecation warnings noticed in the build output:
>    - In `AppDatabase.kt`: use `fallbackToDestructiveMigration(dropAllTables = true)`
>    - In `ChatScreen.kt`: replace deprecated `Icons.Filled.Send` with `Icons.AutoMirrored.Filled.Send`.

---

## 1. Overview of Proposed Changes

### 1. `.github/workflows/build-apk.yml`
Add the `Prepare debug keystore` step immediately before building with Gradle:
```yaml
      - name: Prepare debug keystore
        run: |
          if [ -f debug.keystore.base64 ]; then
            echo "Decoding debug.keystore from base64..."
            base64 -d debug.keystore.base64 > debug.keystore
          elif [ ! -f debug.keystore ]; then
            echo "Generating new debug.keystore with keytool..."
            keytool -genkey -v -keystore debug.keystore \
              -storepass android \
              -alias androiddebugkey \
              -keypass android \
              -keyalg RSA \
              -keysize 2048 \
              -validity 10000 \
              -dname "CN=Android Debug,O=Android,C=US"
          fi
```

### 2. Code Polishing
- Update `AppDatabase.kt` to avoid the deprecation warning on `fallbackToDestructiveMigration`.
- Update `ChatScreen.kt` to use `Icons.AutoMirrored.Filled.Send`.

---

## 2. Execution Plan
1. Update `.github/workflows/build-apk.yml` with the keystore provisioning step.
2. Update `AppDatabase.kt` and `ChatScreen.kt`.
3. Verify compilation locally using `compile_applet`.
