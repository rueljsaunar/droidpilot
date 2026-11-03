# DroidPilot – Architecture & Implementation Plan

**Last updated:** 2026-11-03  
**Status:** Phase 0 – Under Review

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

- `TestDiscovery`: scans project dir for `test_*.py`, collects `test_*` functions; respects `setup`/`teardown`
- `TestExecutor`: runs tests on a background coroutine; streams `LogEvent` objects via `SharedFlow`
- `RunSession`: holds run state; cancellable via `Job.cancel()`
- `RunForegroundService`: wraps a run session; posts a persistent notification with a Stop action
- Retry logic: configurable per-project; exponential back-off between retries
- Timeout: per-test and global; cancels the coroutine and reports `TIMEOUT` status

### 3.6 `:reporting`

- `JUnitXmlWriter`: produces valid JUnit XML (compatible with CI tools and Android Studio)
- `HtmlReportWriter`: self-contained HTML (all screenshots inlined as base64) — no external dependencies
- `ScreenshotCapture`: hooks into the accessibility backend for regular screenshots; Shizuku for higher fidelity
- Room database (`RunHistoryDatabase`): stores `RunRecord`, `TestRecord`, `LogEntry` entities; migration strategy from day one

### 3.7 Feature Modules

| Module | Key screens / components |
|--------|--------------------------|
| `:feature:projects` | Project list, create/rename/delete/import/export dialogs |
| `:feature:editor` | File tree, tab bar, Sora Editor view, run button, console panel |
| `:feature:overlay` | `TYPE_APPLICATION_OVERLAY` window; Run/Stop, step counter, pass/fail; collapsible bubble |
| `:feature:inspector` | Screenshot with tappable element boxes + hierarchy tree; attribute panel; "insert selector" action |
| `:feature:recorder` | Tap/type capture via accessibility events; step list; generate + review Python steps |

---

## 4. Data Flow

```
User taps "Run" in editor
         │
         ▼
  :runner TestExecutor
         │  starts foreground service
         │  launches coroutine
         ▼
  :script:python (Chaquopy)
    executes test_*.py on Python thread
         │
         ▼
  droidpilot Python module
    calls DeviceProxy methods
         │  (JNI bridge)
         ▼
  :core:automation DeviceActions impl
         │  dispatches to active backend(s)
    ┌────┴────┐
    ▼         ▼
accessibility  shizuku
backend        backend
    │         │
    └────┬────┘
         │  emits LogEvent via SharedFlow
         ▼
  RunSession aggregates events
         │
    ┌────┴────┐
    ▼         ▼
  overlay   run console
  (live)    (in editor)
         │
         ▼  (on completion)
  :reporting writes JUnit XML + HTML
  Room stores RunRecord
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

## 7. Dependency Versions (to be verified before adding)

> Versions will be confirmed against current stable releases before each is added to the version catalog. The following are approximate targets based on knowledge as of late 2025; do **not** use without verification.

| Dependency | Approx target | Verified in phase |
|-----------|--------------|-------------------|
| Android Gradle Plugin | 8.x | Phase 0 |
| Kotlin | 2.x | Phase 0 |
| Compose BOM | Latest stable | Phase 0 |
| Hilt | 2.5x | Phase 0 |
| Chaquopy | 16.x | Phase 2 |
| Shizuku API | 13.x | Phase 6 |
| Sora Editor | 0.23.x | Phase 3 |
| ML Kit Text Recognition | 16.x | Phase 9 |
| Room | 2.6.x | Phase 5 |
| Kotlin Coroutines | 1.9.x | Phase 0 |

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
