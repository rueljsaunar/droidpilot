# DroidPilot – Architecture & Implementation Plan

**Last updated:** 2026-11-04  
**Status:** Phase 5 complete — Phases 0–5 shipped, Phase 6 (Shizuku) next

---

## 1. Product Vision

DroidPilot is a self-contained on-device Android QA automation platform. A QA engineer installs one APK, grants permissions once, and can write, run, and debug Python UI automation tests entirely on their phone — no PC, no USB cable, no external server. It mirrors the feel of `uiautomator2`/Appium so existing test suites port with minimal changes.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│  :app  (Navigation, DI wiring, permissions, onboarding)     │
│                                                              │
│  Feature Modules                                             │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────┐ │
│  │:f:editor │ │:f:project│ │:f:inspector│ │:f:recorder   │ │
│  │:f:overlay│ └──────────┘ └────────────┘ └──────────────┘ │
│  └──────────┘                                                │
│                                                              │
│  ┌──────────────────────────────────┐                        │
│  │  :runner  (discovery, lifecycle) │                        │
│  │  :reporting (JUnit XML, HTML)    │                        │
│  └──────────────────────────────────┘                        │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  :core:automation  (backend-agnostic API contract)    │  │
│  │  Selector · UiElement · DeviceActions · Waits         │  │
│  └───────┬───────────────┬──────────────────┬────────────┘  │
│          │               │                  │               │
│  ┌───────▼──────┐ ┌──────▼──────┐ ┌─────────▼──────────┐   │
│  │:b:access-    │ │:b:shizuku   │ │:b:vision           │   │
│  │ibility       │ │(shell cmds) │ │(MediaProj+ML Kit)  │   │
│  └──────────────┘ └─────────────┘ └────────────────────┘   │
│                                                              │
│  ┌────────────────────────────────┐                          │
│  │  :script:python (Chaquopy)     │                          │
│  │  droidpilot Python module      │                          │
│  └────────────────────────────────┘                          │
│                                                              │
│  ┌────────────────────────────────┐                          │
│  │  :sample-target                │                          │
│  │  (demo app for dev/self-tests) │                          │
│  └────────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

### Layer responsibilities

| Layer | Responsibility |
|-------|----------------|
| **:app** | Compose navigation graph, Hilt component root, permission flows, onboarding, deep-link handling |
| **:feature/*** | UI screens and ViewModels for each feature; depend only on `:core:automation` interfaces and use-cases |
| **:runner** | Test discovery, execution lifecycle, timeouts, retries, cancellation, foreground service |
| **:reporting** | JUnit XML writer, HTML report generator, screenshot embedding, Room-backed run history |
| **:core:automation** | Backend-agnostic interfaces: `Selector`, `UiElement`, `DeviceActions`, `WaitStrategy`; no Android framework imports except minimal ones |
| **:backend:accessibility** | `AccessibilityService` implementation; UI tree traversal, node actions, `dispatchGesture`, global key events |
| **:backend:shizuku** | Shell-level actions via Shizuku API; `am`, `pm`, `input`, `screencap`, `screenrecord`, `logcat`, `uiautomator dump` |
| **:backend:vision** | MediaProjection capture, ML Kit OCR, template matching (Phase 9) |
| **:script:python** | Chaquopy integration; `droidpilot` Python package; bridge between Python API calls and Kotlin `DeviceActions` |
| **:sample-target** | Small demo app: login form, item list, dialogs, WebView; used as app-under-test in self-tests |

---

## 3. Module Details

### 3.1 `:core:automation`

Central contracts. No implementation.

```
core/automation/
  src/main/kotlin/com/droidpilot/core/automation/
    Selector.kt           // sealed class hierarchy for all selector types
    UiElement.kt          // interface: exists, click, getText, bounds, wait, …
    DeviceActions.kt      // interface: tap, swipe, appStart/Stop/Clear, shell, …
    WaitStrategy.kt       // event-driven + polling fallback
    AutomationException.kt  // BackendUnavailableError, ElementNotFoundError, …
    BackendProvider.kt    // interface; implementations in backend modules
```

**Selector hierarchy:**

```kotlin
sealed class Selector {
    data class Text(val value: String) : Selector()
    data class TextContains(val value: String) : Selector()
    data class TextMatches(val pattern: String) : Selector()
    data class ResourceId(val id: String) : Selector()
    data class Description(val value: String) : Selector()
    data class ClassName(val name: String) : Selector()
    data class Package(val name: String) : Selector()
    data class Clickable(val value: Boolean) : Selector()
    data class Index(val index: Int) : Selector()
    data class Instance(val n: Int) : Selector()
    data class XPath(val expression: String) : Selector()
    data class Child(val parent: Selector, val child: Selector) : Selector()
    data class Sibling(val anchor: Selector, val sibling: Selector) : Selector()
    data class Compound(val parts: List<Selector>) : Selector()
}
```

### 3.2 `:backend:accessibility`

- Extends `AccessibilityService`; declared in manifest with required config XML
- Holds a singleton snapshot updated on every `TYPE_WINDOW_CONTENT_CHANGED` / `TYPE_WINDOWS_CHANGED` event
- `AccessibilityNodeInfo` objects are explicitly recycled after every query to prevent leaks
- Selector engine: depth-first traversal with early-exit; benchmarked at < 100 ms on typical screens
- Gesture dispatch via `AccessibilityService.dispatchGesture` with `GestureDescription`

### 3.3 `:backend:shizuku`

- Uses `rikka.shizuku:shizuku-api` and `rikka.shizuku:shizuku-provider`
- Binds to Shizuku service; all calls are async (coroutine wrappers)
- Raises `BackendUnavailableError` with human-readable guidance when Shizuku is unavailable
- Key commands: `am start/force-stop`, `pm clear/install`, `screencap -p`, `screenrecord`, `logcat -d`, `uiautomator dump`

### 3.4 `:script:python`

- Chaquopy plugin configured in the `:script:python` module's `build.gradle.kts`
- `droidpilot` Python package lives in `src/main/python/droidpilot/`
- `DeviceProxy` Python class: each method call crosses the JNI bridge to the Kotlin `DeviceActions` impl
- `expect(element)` returns an `Expectation` builder with `.to_be_visible(timeout)`, `.to_have_text(...)`, etc.
- Failed assertions auto-capture screenshot + UI tree dump via the Kotlin side

### 3.5 `:runner`

- `TestDiscovery`: regex-based JVM scan of `test_*.py` files; collects `def test_*` functions; detects `setup`/`teardown`; no Chaquopy required at discovery time
- `TestExecutor`: `Flow<RunEvent>` pipeline; `withTimeout` + configurable retries per test
- `RunRepository`: `@Singleton` Hilt component; two SharedFlows — `events(replay=100)` for overlay/console, `recordEvents(replay=0)` for `ReportRepository` to avoid stale-history re-processing; aggregates `isRunning`, `passCount`, `failCount`, `currentStep` StateFlows
- `RunForegroundService`: `@AndroidEntryPoint`; `foregroundServiceType="dataSync"`; `ACTION_START`/`ACTION_STOP` intent API; passes `EXTRA_PROJECT_ID` + `EXTRA_PROJECT_DIR` to `runRepository.startRun()`
- `RunConfig`: `timeoutMs`, `maxRetries`; defaults 30 s / 0 retries
- `RunEvent` sealed interface: `RunStarted(projectId, projectDir)`, `TestStarted`, `TestPassed`, `TestFailed`, `StepLogged`, `RunComplete`

### 3.6 `:reporting`

- **Room DB v1**: `RunRecord`, `TestRecord`, `LogEntry` with `CASCADE` FK deletes; KSP schema export to `reporting/schemas/`
- `ReportingDao`: typed queries — runs by project (Flow), tests/logs by run (suspend)
- `ReportRepository`: `@Singleton`; subscribes to `RunRepository.recordEvents` at app startup; accumulates in-memory per-run, writes to Room + files on `RunComplete`
- `JunitXmlWriter`: valid JUnit XML grouped by classname (file basename); `<testsuites>` + `<testsuite>` + `<testcase>` with `<failure>` elements; UTF-8 escaped
- `HtmlReportWriter`: dark-mode-first self-contained HTML; stat tiles, test result table, log section; no external dependencies; screenshot fields nullable (MediaProjection deferred to Phase 9)
- Reports written to `filesDir/reports/{runId}/report.xml` and `report.html`; paths stored in `RunRecord`
- `FileProvider` declared in manifest (`{packageName}.fileprovider`) exposes `filesDir/reports/` for sharing

### 3.7 Feature Modules

| Module | Key screens / components | Status |
|--------|--------------------------|--------|
| `:feature:projects` | Project list (CRUD, rename, delete), project settings (target app, run config), run history (per-project list of runs with share action) | ✅ Phase 3+5 |
| `:feature:editor` | File tree sidebar, tab bar + autosave (500 ms debounce), Sora Editor with Python TextMate grammar, run button, run console panel | ✅ Phase 3 |
| `:feature:overlay` | `TYPE_APPLICATION_OVERLAY` via `WindowManager`; collapsible/draggable bubble; live step, pass/fail counters; `OverlayLifecycleOwner` for hosted `ComposeView` | ✅ Phase 4 |
| `:feature:inspector` | Screenshot with tappable element boxes + hierarchy tree; attribute panel; "insert selector" action | ⬜ Phase 7 |
| `:feature:object-repository` | Saved selectors; `RepositoryEntry` Room entity; search/filter; insert into editor | ⬜ Phase 7 |
| `:feature:recorder` | Tap/type capture via accessibility events; step list; generate + review Python steps | ⬜ Phase 8 |

---

## 4. Data Flow

```
User taps "Run" in editor
         │
         ▼
  EditorViewModel.runCurrentFile()
         │  calls RunRepository.startRun(projectId, projectDir)
         │  emits RunEvent.RunStarted
         │  calls ScriptRunner.executeCapturingOutput(script)
         ▼
  :script:python (Chaquopy)
    executes Python on Chaquopy thread; stdout/stderr captured
         │
         ▼  (or via RunForegroundService for full suites)
  TestExecutor.execute(tests, config): Flow<RunEvent>
         │  withTimeout + retries per test
         ▼
  ScriptRunner.executeCapturingOutput(buildScript(testCase))
    builds Python that imports & calls the test function
         │
         ▼
  RunRepository.emit(event)
    ├── _events (replay=100) ──────────────────────┐
    │                                               ▼
    │                                    OverlayService  (live bubble)
    │                                    EditorScreen    (run console)
    │
    └── _recordEvents (replay=0) ─────────────────▶ ReportRepository
                                                      accumulates per-run
                                                      ▼ on RunComplete
                                                    Room (RunRecord, TestRecord, LogEntry)
                                                    JunitXmlWriter → filesDir/reports/{id}/report.xml
                                                    HtmlReportWriter → filesDir/reports/{id}/report.html
                                                      ▼
                                                    RunHistoryScreen (share via FileProvider)
```

---

## 5. Key Design Decisions

### 5.1 Backend Abstraction

`DeviceActions` is a Kotlin interface. At startup, `BackendOrchestrator` builds a priority-ordered chain:

1. Shizuku (if bound)
2. Accessibility service
3. Vision / OCR (Phase 9)

Each action is attempted in priority order; if the top backend raises `BackendUnavailableError`, the next is tried. This means Shizuku can enhance but is never required.

### 5.2 Accessibility Node Lifecycle

All `AccessibilityNodeInfo` objects are wrapped in an `AutoCloseable` holder that recycles them on close. All tree-walking code uses `use {}` blocks. This is enforced by a custom Detekt rule.

### 5.3 Password Field Masking

Before logging any `getText` result, the automation layer checks `AccessibilityNodeInfo.isPassword`. Masked fields are logged as `"[REDACTED]"`. The Python bridge never receives raw password text.

### 5.4 Thread Model

| Context | Thread |
|---------|--------|
| Compose UI | Main thread |
| Python test execution | Chaquopy's Python thread |
| Accessibility callbacks | Accessibility thread → immediately posted to a `Channel` |
| Shell commands | `Dispatchers.IO` coroutine |
| File I/O | `Dispatchers.IO` coroutine |

The `RunSession` bridges Python callbacks to Kotlin coroutines via a `Channel<LogEvent>` consumed on `Dispatchers.Default`.

### 5.5 Foreground Service for Long Runs

A `RunForegroundService` is started before every test run. This prevents the OS from killing the process during long suites and keeps a persistent "Running tests – tap to stop" notification.

### 5.6 Robustness Patterns

- **Stale node detection:** if `AccessibilityNodeInfo.refresh()` returns false, retry the selector with a fresh snapshot
- **Window change handling:** wait for `TYPE_WINDOWS_CHANGED` after every action before the next selector lookup
- **App crash detection:** monitor `TYPE_WINDOW_STATE_CHANGED`; if the target package disappears, emit `TargetCrashedError` and fail fast
- **Keyboard overlay:** before tapping an element covered by the soft keyboard, check bounds intersection and request keyboard dismissal first

---

## 6. Permissions & Privacy

| Permission | Why needed | When requested |
|-----------|-----------|----------------|
| `BIND_ACCESSIBILITY_SERVICE` | UI tree + gestures | Onboarding step 1; deep-links to system settings |
| `SYSTEM_ALERT_WINDOW` | Floating overlay | Onboarding step 2 |
| `POST_NOTIFICATIONS` | Run progress + stop action | Onboarding step 3 (Android 13+) |
| `FOREGROUND_SERVICE` | Long-running test suites | Declared; no runtime prompt |
| `REQUEST_INSTALL_PACKAGES` | `app_install()` in scripts | Requested on first use |
| Shizuku API | Shell-level actions | Onboarding step 4 (optional) |
| `MEDIA_PROJECTION` | High-fidelity screenshots | Requested per-run (Phase 9) |

**Privacy guarantees:**
- No network calls (except user scripts)
- No telemetry
- Password fields always masked in logs
- All data stays in app-specific storage

---

## 7. Dependency Versions (locked in `gradle/libs.versions.toml`)

| Dependency | Version in use | Introduced |
|-----------|----------------|-----------|
| Android Gradle Plugin | 9.4.0 | Phase 0 |
| Kotlin | 2.3.21 | Phase 0 |
| KSP | 2.3.9 | Phase 0 |
| Compose BOM | 2026.09.00 | Phase 0 |
| Hilt | 2.60.1 | Phase 0 |
| Navigation Compose | 2.9.8 | Phase 0 |
| Coroutines | 1.11.0 | Phase 0 |
| Chaquopy | 17.2.0 | Phase 2 |
| Sora Editor | 0.23.6 | Phase 3 |
| Room | 2.8.5 | Phase 3 (projects DB), Phase 5 (reporting DB) |
| Shizuku API | TBD | Phase 6 |
| ML Kit Text Recognition | TBD | Phase 9 |

Verify new versions at the sources listed in `CLAUDE.md` before adding any library.

---

## 8. Known Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Accessibility service killed by battery optimization | Onboarding prompts user to exempt app; foreground service for runs |
| Chaquopy adds significant APK size | Accept; document minimum device storage requirement |
| `FLAG_SECURE` apps block accessibility tree | Detect and report clearly; offer coordinate-based fallback |
| Flutter / game / canvas UIs have no accessibility tree | Fall back to Vision/OCR (Phase 9); log warning |
| Shizuku stopped after reboot | Detect state at startup; show banner with re-enable instructions |
| Android 13+ "Restricted settings" blocks accessibility grant | Onboarding includes guide for "Allow restricted settings" via App Info |
| Chaquopy Python thread and Kotlin coroutine synchronization | All bridging through `Channel`; no shared mutable state across threads |
| Room schema migrations | Define `AutoMigration` from schema version 1; never use `fallbackToDestructiveMigration` |

---

## 9. Phase Checklist

See [CLAUDE.md](../CLAUDE.md) for the full phase checklist with build-pass gates.

---

## 10. Resolved Decisions

| # | Question | Decision | Rationale |
|---|----------|----------|-----------|
| 1 | Chaquopy Python version | **Newest version Chaquopy supports** (verify at project scaffold) | Future-proof; avoids 3.8 EOL; better type hints in API |
| 2 | Overlay process isolation | **Same process** | Lower complexity; main process crashes are caught and reported as test failures anyway |
| 3 | `:sample-target` WebView | **Include from Phase 0** | Lets us validate OCR fallback early without rework; minimal cost to add now |
| 4 | Editor autocompletion | **Bundled Python LSP server** (e.g., Pylsp or Pyright for Android) | Richer completions, hover docs, go-to-definition; APK size is acceptable for internal distribution |
