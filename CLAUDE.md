# DroidPilot – Developer Guide

## Quick Reference

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Run unit tests | `./gradlew test` |
| Run instrumented tests | `./gradlew connectedDebugAndroidTest` |
| Lint | `./gradlew ktlintCheck detekt` |
| Auto-fix lint | `./gradlew ktlintFormat` |
| Build all modules | `./gradlew build` |
| Clean | `./gradlew clean` |

---

## Module Map

```
DroidPilot/
├── app/                        # Navigation, DI root, permissions, onboarding
├── core/
│   └── automation/             # Backend-agnostic API: Selector, UiElement, DeviceActions
├── backend/
│   ├── accessibility/          # AccessibilityService implementation
│   ├── shizuku/                # Shell-level actions via Shizuku
│   └── vision/                 # MediaProjection, ML Kit OCR (Phase 9)
├── script/
│   └── python/                 # Chaquopy integration, droidpilot Python package
├── runner/                     # Test discovery, execution, foreground service
├── reporting/                  # JUnit XML, HTML reports, Room run history
├── feature/
│   ├── editor/                 # Sora Editor integration, file tree, run console
│   ├── projects/               # Project CRUD, import/export
│   ├── overlay/                # Floating overlay window
│   ├── inspector/              # UI tree inspector
│   └── recorder/               # Tap/text recorder → Python steps
└── sample-target/              # Demo app used as app-under-test
```

---

## Coding Conventions

### Kotlin

- **Kotlin 2.x**, explicit API mode on all non-app modules
- **Coroutines + Flow** for all async; no `RxJava`, no `LiveData` (except where Compose requires)
- **Hilt** for DI; no manual service locators
- **Compose + Material 3** for all UI
- No `lateinit var` unless justified; prefer `val` and constructor injection
- Sealed classes/interfaces for domain types (especially `Selector`, `AutomationResult`)
- All public API has KDoc
- `AccessibilityNodeInfo` objects must be recycled; always wrap in `AutoCloseable` holder and use `use {}`

### Naming

| Kind | Convention | Example |
|------|-----------|---------|
| Files | PascalCase | `SelectorEngine.kt` |
| Interfaces | noun or noun phrase | `DeviceActions` |
| Implementations | Interface + suffix | `AccessibilityDeviceActions` |
| ViewModels | Screen + ViewModel | `EditorViewModel` |
| Composables | PascalCase, no suffix | `RunConsole` |
| Flows/State | camelCase | `runState`, `logEvents` |

### Python (droidpilot package)

- Python 3.8 target (Chaquopy compatibility)
- All public methods have docstrings (used for editor autocompletion)
- No mutable global state in the bridge module
- Raise `DroidPilotError` (or subclass) for all automation failures; never swallow exceptions

---

## Architecture Rules

1. **No direct dependencies downward through layers.** `:feature/*` modules depend on `:core:automation` interfaces only — never on `:backend/*` implementations.
2. **No Android framework imports in `:core:automation`.** That module is pure Kotlin + coroutines.
3. **No password text in logs.** Check `isPassword` before logging any text value.
4. **No hard sleeps** (`Thread.sleep`, `delay` for polling) in the framework. Use `WaitStrategy` (event-driven + polling fallback).
5. **All `AccessibilityNodeInfo` objects recycled.** Enforced by Detekt rule `AccessibilityNodeInfoLeak`.

---

## Phase Checklist

Mark each item complete before committing a phase. All items in a phase must pass before starting the next.

### Phase 0 – Scaffold ☐
- [ ] Multi-module Gradle project created (Kotlin DSL, version catalog)
- [ ] All modules in module map exist as empty stubs
- [ ] Hilt wired in `:app`
- [ ] Compose app shell renders a placeholder screen
- [ ] `:sample-target` builds and runs (login form + item list + dialog)
- [ ] `./gradlew assembleDebug` passes (all modules)
- [ ] `./gradlew test` passes (trivial tests)
- [ ] `CLAUDE.md` created ✅
- [ ] `docs/ARCHITECTURE.md` created ✅

### Phase 1 – Accessibility Backend ☐
- [ ] `AccessibilityService` declared in manifest with correct config
- [ ] UI tree snapshot updated on window events
- [ ] `SelectorEngine` resolves all `Selector` types against a serialized tree
- [ ] `UiElement` actions: `click`, `longClick`, `setText`, `getText`, `exists`, `bounds`
- [ ] `WaitStrategy`: event-driven wait + polling fallback; `wait()` and `waitGone()`
- [ ] Gesture dispatch: `tap`, `swipe`, `drag`, `pinch` via `dispatchGesture`
- [ ] Unit tests: selector engine against 5+ serialized UI tree fixtures
- [ ] Instrumented test: drives `:sample-target` login flow end-to-end
- [ ] `AccessibilityNodeInfo` leak Detekt rule active; no leaks in tests

### Phase 2 – Python Bridge ☐
- [ ] Chaquopy configured in `:script:python` `build.gradle.kts`
- [ ] `droidpilot` Python package in `src/main/python/`
- [ ] `DeviceProxy` bridges all Phase 1 actions to Python
- [ ] `expect(element)` with `to_be_visible(timeout)` works
- [ ] Hardcoded script against `:sample-target` runs end-to-end

### Phase 3 – Editor & Projects ☐
- [ ] `:feature:projects`: create, rename, delete, import zip, export zip
- [ ] `:feature:editor`: Sora Editor with Python syntax highlighting, line numbers
- [ ] File tree sidebar; tabs; autosave (debounced 500 ms)
- [ ] Run button executes current file; output shown in run console
- [ ] `./gradlew test` and `assembleDebug` still pass

### Phase 4 – Runner & Overlay ☐
- [ ] `TestDiscovery` finds `test_*.py` / `test_*` functions; respects `setup`/`teardown`
- [ ] Run single test, file, or suite
- [ ] Per-test timeout; configurable retries
- [ ] `RunForegroundService` with persistent notification + Stop action
- [ ] Cancel at any time; coroutine cancelled cleanly
- [ ] `:feature:overlay`: draggable, collapsible bubble; shows current step, pass/fail counter
- [ ] Live log stream to overlay and run console

### Phase 5 – Reporting & History ☐
- [ ] JUnit XML written after each run; valid schema
- [ ] HTML report: self-contained, screenshots inlined as base64
- [ ] Screenshots captured on assertion failure
- [ ] Room database: `RunRecord`, `TestRecord`, `LogEntry` with schema version 1
- [ ] `:feature:projects` run history tab: list runs, open report, share zip

### Phase 6 – Shizuku Backend ☐
- [ ] Shizuku API bound; state detected at startup; banner shown if unavailable
- [ ] `app_start`, `app_stop`, `app_clear`, `app_install`, `shell(cmd)` implemented
- [ ] `screencap` and `logcat -d` capture per test
- [ ] Graceful fallback: `BackendUnavailableError` with setup instructions when Shizuku absent
- [ ] Onboarding guide for Wireless Debugging + Shizuku

### Phase 7 – Inspector ☐
- [ ] Capture current screen snapshot from overlay
- [ ] Screenshot with tappable element bounding boxes overlay
- [ ] Hierarchy tree view; selecting node highlights box
- [ ] Attribute panel: all relevant `AccessibilityNodeInfo` fields
- [ ] Selector suggestion: most-stable selector ranked first
- [ ] "Copy selector" and "Insert into editor" actions

### Phase 8 – Recorder ☐
- [ ] Start/stop recording from overlay
- [ ] Capture tap positions and text input via accessibility events
- [ ] Generate Python step for each captured action (best available selector)
- [ ] Review/edit step list before saving
- [ ] Save steps at cursor position in active editor tab

### Phase 9 – Vision / OCR ☐
- [ ] MediaProjection screen capture integrated
- [ ] ML Kit Text Recognition: `d.ocr.find_text("Login").click()` works
- [ ] Simple image template matching
- [ ] FLAG_SECURE apps detected; fallback path used automatically; logged clearly

### Phase 10 – Polish ☐
- [ ] Sora Editor autocompletion for `droidpilot` API (static completions or LSP)
- [ ] Project templates (blank, login-test, e2e-suite)
- [ ] `docs/USER_GUIDE.md` written
- [ ] `docs/API.md` written (all Python API methods documented)
- [ ] Example test suite for `:sample-target` included
- [ ] All KDoc on public Kotlin API complete
- [ ] All Python docstrings complete

---

## Dependency Version Verification

Before adding any library, verify the current stable version at its source. Do **not** use remembered version numbers.

| Library | Source to check |
|---------|----------------|
| Android Gradle Plugin | https://developer.android.com/studio/releases/gradle-plugin |
| Kotlin | https://kotlinlang.org/docs/releases.html |
| Compose BOM | https://developer.android.com/jetpack/compose/bom/bom-mapping |
| Chaquopy | https://chaquo.com/chaquopy/doc/current/changelog.html |
| Shizuku API | https://github.com/RikkaApps/Shizuku/releases |
| Sora Editor | https://github.com/Rosemoe/sora-editor/releases |
| ML Kit | https://developers.google.com/ml-kit/release-notes |

---

## Key Files

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Version catalog (single source of truth for all dependency versions) |
| `docs/ARCHITECTURE.md` | Architecture, data flow, design decisions, open questions |
| `core/automation/src/main/kotlin/…/Selector.kt` | Selector sealed class hierarchy |
| `core/automation/src/main/kotlin/…/DeviceActions.kt` | Backend-agnostic action interface |
| `backend/accessibility/src/main/kotlin/…/DroidPilotAccessibilityService.kt` | AccessibilityService implementation |
| `script/python/src/main/python/droidpilot/__init__.py` | Python API entry point |
| `runner/src/main/kotlin/…/TestExecutor.kt` | Core execution engine |
