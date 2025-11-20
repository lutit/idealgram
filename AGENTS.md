# Repository Guidelines

## Project Structure & Module Organization
- `TMessagesProj/`: main Android app module; app source lives under `src/main/java` and `src/main/res`.
- `TMessagesProj/jni/`: native C/C++ sources and CMake configuration.
- `Tools/`: helper scripts and HTML utilities used during development.
- `.github/workflows/`: CI pipelines for test, staging, and release; keep these green when changing build logic.

## Build, Test, and Development Commands
- `./gradlew :TMessagesProj:assembleDebug` – build a debuggable APK for local installs.
- `./gradlew :TMessagesProj:assembleRelease` – build a signed release APK (requires valid keystore and `local.properties`).
- `./gradlew :TMessagesProj:lintDebug` – run Android Lint checks for the debug variant.
- `./gradlew :TMessagesProj:testDebugUnitTest` – run JVM unit tests.
- `./gradlew :TMessagesProj:connectedDebugAndroidTest` – run instrumentation tests on a connected device or emulator.

## Coding Style & Naming Conventions
- Use Android Studio’s default formatter (Kotlin/Java, 4-space indentation, braces on the same line).
- Classes: `PascalCase`; methods, fields, and locals: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Keep UI strings and user-facing text in `res/values/strings.xml` (Crowdin-managed where applicable).
- For native code under `jni/`, match existing style (4 spaces, `snake_case` helpers, const-correctness).

## Testing Guidelines
- Prefer small, fast unit tests for business logic; use instrumentation tests only when Android framework behavior is required.
- Place tests in `src/test/java` and `src/androidTest/java`, mirroring the package of the code under test.
- New features should include tests when feasible and must not break existing CI workflows.

## Commit & Pull Request Guidelines
- Use conventional-style summaries when possible: `type(scope): short imperative message`, e.g. `fix(ui): avoid crash on theme load`.
- Keep the first line ≤72 characters; explain rationale and side effects in the body using bullets.
- For PRs, include: short summary, motivation, key changes, testing performed, and any configuration or migration notes.
- Link related issues and attach screenshots or screen recordings for visible UI changes.

## Security & Configuration
- Never commit real secrets, personal `local.properties`, or private keystores; use placeholders and `.b64`/CI secrets as described in `README.md`.
- When sharing logs or configs for debugging, remove or redact any sensitive values first.

## Current Repo Context
- Root Gradle uses AGP `8.13.0`, Kotlin `2.2.21`, and Java/Kotlin `21` targeting `compileSdk 36`, `minSdk 27`, `targetSdk 36`, with `buildTools 36.0.0` and `ndk 27.2.12479018`.
- `TMessagesProj/build.gradle` injects signing/API credentials from `local.properties` or `LOCAL_PROPERTIES` secret; release/staging/debug all share the release keystore at `TMessagesProj/release.keystore`.
- `applicationVariants` rename APKs to `IdealGram-v<version>(<code>)` and `splits.abi` obeys the `NATIVE_TARGET` env (set to `x86_64` for quick tests, CI matrix overrides per ABI).
- `google-services.json` currently targets package `au.idealgram.org`; replace with project-specific config before distributing.
- `.github/workflows/test.yml` runs `assembleStaging` with cached Gradle and Android 36 SDK, while `.github/workflows/release.yml` loops ABIs, enables ProGuard optimization, and expects `LOCAL_PROPERTIES`/custom keystore secrets.
