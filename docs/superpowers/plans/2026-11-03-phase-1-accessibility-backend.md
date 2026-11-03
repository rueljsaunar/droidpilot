# Phase 1 – Accessibility Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `:backend:accessibility` module — a fully functional `AccessibilityService` that resolves `Selector` queries against the live UI tree, performs element actions, dispatches gestures, and exposes a `WaitStrategy` for event-driven synchronization. Unit tests cover all selector types via serialized fixtures; one instrumented test drives the `:sample-target` login flow end-to-end.

**Architecture:** `DroidPilotAccessibilityService` holds a continuously updated `UiTreeSnapshot` (a serialized copy of the live tree). `SelectorEngine` operates on `SerializedNode` trees (pure Kotlin, no Android imports — fully unit-testable). `AccessibilityUiElement` re-resolves its `Selector` from the snapshot on each action call. `WaitStrategy` interface lives in `:core:automation`; `AccessibilityWaitStrategy` emits a tick on every window event and retries the condition until satisfied or timed out.

**Tech Stack:** Kotlin coroutines, Hilt, `AccessibilityService`, `GestureDescription`, `AccessibilityNodeInfo`, Detekt custom rule

**Spec:** [docs/ARCHITECTURE.md](../../ARCHITECTURE.md)

---

## Global Constraints

- All public API in `:core:automation` uses `public` modifier (explicit API mode)
- `:core:automation` must remain free of Android framework imports
- All `AccessibilityNodeInfo` objects must be recycled via `NodeHolder.use {}` — never held outside a `use {}` block
- No `Thread.sleep` or bare `delay()` for polling; use `WaitStrategy` exclusively
- Password fields (`isPassword == true`) must always return `"[REDACTED]"` from `getText()`; never log raw text
- Minimum SDK 26 (`minSdk` from version catalog); `AccessibilityService.takeScreenshot()` requires API 30 — detect and throw `BackendUnavailableError` on older devices
- All new dependencies verified against current stable releases before adding to `libs.versions.toml`

---

## Review Focus

1. **NodeHolder closed before action completes** — if `NodeHolder` is closed while a `performAction` call is still in-flight, the node is recycled and the action crashes. Test: action body must complete before `use {}` exits; never close inside a launch/async.
2. **WaitStrategy race: event fires between condition check and subscribe** — if the window event fires *before* the wait loop subscribes, the condition update is missed and the wait times out. Fix: subscribe first, then check the condition; use `SharedStateFlow` (replay=1) not a plain `MutableSharedFlow`.
3. **SelectorEngine Index vs Instance semantics** — `Index` is the node's position among its parent's children; `Instance` is the nth match globally across the whole tree. These are easily confused. Each has its own test fixture.
4. **XPath selector throws `UnsupportedOperationException`** — callers must get a clear error, not a silent null. Verify the test exercises the throw.
5. **Instrumented test flakiness on slow devices** — 5 000 ms element waits may be too short on a freshly-booted emulator. Default wait is configurable; tests use `10_000` ms.

---

## Task 1: Infrastructure — WaitStrategy, NodeHolder, SerializedNode, build files

**Files:**
- Create: `core/automation/src/main/kotlin/com/droidpilot/core/automation/WaitStrategy.kt`
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/NodeHolder.kt`
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/SerializedNode.kt`
- Modify: `core/automation/build.gradle.kts` — add coroutines-core dependency
- Modify: `backend/accessibility/build.gradle.kts` — add Hilt, KSP, coroutines, test deps
- Create: `core/automation/src/test/kotlin/com/droidpilot/core/automation/WaitStrategyTest.kt`

**Interfaces:**
- Produces: `WaitStrategy` interface (consumed by Task 4 UiElement and all future backends), `NodeHolder` (consumed by Tasks 2–5), `SerializedNode` (consumed by Task 3 SelectorEngine)

- [ ] **Step 1: Write failing test in `WaitStrategyTest.kt`**

  ```kotlin
  class WaitStrategyTest {
      @Test
      fun `await returns true when condition becomes true before timeout`()
      @Test
      fun `await returns false when condition stays false until timeout`()
      @Test
      fun `await returns true immediately when condition already true`()
  }
  ```
  Run: `./gradlew :core:automation:test`
  Expected: FAIL — `WaitStrategy` not defined

- [ ] **Step 2: Write `WaitStrategy.kt` in `core/automation`**

  Interface with one method:
  ```kotlin
  public interface WaitStrategy {
      public suspend fun await(timeoutMs: Long, condition: suspend () -> Boolean): Boolean
  }
  ```
  No implementation here — the interface only. No Android imports.

- [ ] **Step 3: Write `PollingWaitStrategy` in core:automation for tests**

  A simple polling-only implementation used only in unit tests (not shipped to production):
  - Calls `condition()` every `intervalMs` (default 50) until it returns true or `timeoutMs` elapses
  - Use `kotlinx.coroutines.delay` and `withTimeoutOrNull`

- [ ] **Step 4: Run test — verify it passes**

  Run: `./gradlew :core:automation:test`
  Expected: PASS (existing 3 tests + 3 new WaitStrategy tests = 6 total)

- [ ] **Step 5: Update `core/automation/build.gradle.kts`**

  Add `implementation(libs.coroutines.core)` (add `coroutines-core` alias to version catalog pointing to `org.jetbrains.kotlinx:kotlinx-coroutines-core`, same version as `coroutines`).

- [ ] **Step 6: Write `NodeHolder.kt` in `backend/accessibility`**

  ```kotlin
  internal class NodeHolder private constructor(
      private val node: AccessibilityNodeInfo,
  ) : AutoCloseable {
      val info: AccessibilityNodeInfo get() = node
      override fun close() { node.recycle() }
      companion object {
          fun of(node: AccessibilityNodeInfo?): NodeHolder? = node?.let { NodeHolder(it) }
          fun require(node: AccessibilityNodeInfo?): NodeHolder =
              of(node) ?: error("AccessibilityNodeInfo was null")
      }
  }
  ```

- [ ] **Step 7: Write `SerializedNode.kt` in `backend/accessibility`**

  Pure Kotlin data class — NO Android imports:
  ```kotlin
  internal data class SerializedNode(
      val text: String?,
      val contentDescription: String?,
      val resourceId: String?,
      val className: String?,
      val packageName: String?,
      val isClickable: Boolean,
      val isEnabled: Boolean,
      val isPassword: Boolean,
      val isChecked: Boolean,
      val bounds: Bounds,
      val indexInParent: Int,
      val children: List<SerializedNode>,
  )
  ```

- [ ] **Step 8: Update `backend/accessibility/build.gradle.kts`**

  Add `hilt` + `ksp` plugins. Add dependencies:
  - `implementation(libs.hilt.android)` + `ksp(libs.hilt.compiler)`
  - `implementation(libs.coroutines.android)`
  - `testImplementation(libs.junit)` + `testImplementation(libs.coroutines.test)` (add `coroutines-test` alias)
  - `androidTestImplementation(libs.hilt.android.testing)` (add alias) + `kspAndroidTest(libs.hilt.compiler)`
  - `androidTestImplementation(libs.androidx.test.runner)` (add alias: `androidx.test:runner`)
  - `androidTestImplementation(libs.androidx.test.ext.junit)` (add alias: `androidx.test.ext:junit`)
  - Add `defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }`

- [ ] **Step 9: Commit**

  ```
  git add core/automation/ backend/accessibility/build.gradle.kts gradle/libs.versions.toml
  git commit -m "feat(phase-1): WaitStrategy interface, NodeHolder, SerializedNode"
  ```

---

## Task 2: DroidPilotAccessibilityService + UiTreeSnapshot

**Files:**
- Create: `backend/accessibility/src/main/res/xml/accessibility_service_config.xml`
- Modify: `backend/accessibility/src/main/AndroidManifest.xml`
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/UiTreeSnapshot.kt`
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/DroidPilotAccessibilityService.kt`
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/AccessibilityWaitStrategy.kt`
- Delete: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/Placeholder.kt`
- Test: `backend/accessibility/src/test/kotlin/com/droidpilot/backend/accessibility/UiTreeSnapshotTest.kt`

**Interfaces:**
- Produces: `DroidPilotAccessibilityService` (singleton, accessed via companion), `UiTreeSnapshot` (current serialized tree), `AccessibilityWaitStrategy` (consumed by Task 4)

- [ ] **Step 1: Write failing test in `UiTreeSnapshotTest.kt`**

  Tests that `serialize(AccessibilityNodeInfo)` produces a `SerializedNode` tree with correct field mappings. Uses `AccessibilityNodeInfo.obtain()` to create test nodes.

  ```kotlin
  class UiTreeSnapshotTest {
      @Test
      fun `serializes node text and resourceId`()
      @Test
      fun `serializes isPassword flag`()
      @Test
      fun `serializes children recursively`()
  }
  ```
  Run: `./gradlew :backend:accessibility:test`
  Expected: FAIL — `UiTreeSnapshot` not defined

- [ ] **Step 2: Write `accessibility_service_config.xml`**

  ```xml
  <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
      android:accessibilityEventTypes="typeAllMask"
      android:accessibilityFeedbackType="feedbackGeneric"
      android:accessibilityFlags="flagReportViewIds|flagRequestEnhancedWebAccessibility|flagRetrieveInteractiveWindowsContent"
      android:canPerformGestures="true"
      android:canRetrieveWindowContent="true"
      android:description="@string/accessibility_service_description"
      android:notificationTimeout="100" />
  ```
  Add string `accessibility_service_description` to `res/values/strings.xml`.

- [ ] **Step 3: Update `backend/accessibility/src/main/AndroidManifest.xml`**

  Declare the service with `BIND_ACCESSIBILITY_SERVICE` permission and the config XML meta-data:
  ```xml
  <manifest xmlns:android="http://schemas.android.com/apk/res/android">
      <application>
          <service
              android:name=".DroidPilotAccessibilityService"
              android:exported="true"
              android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
              <intent-filter>
                  <action android:name="android.accessibilityservice.AccessibilityService" />
              </intent-filter>
              <meta-data
                  android:name="android.accessibilityservice"
                  android:resource="@xml/accessibility_service_config" />
          </service>
      </application>
  </manifest>
  ```

- [ ] **Step 4: Write `UiTreeSnapshot.kt`**

  - `fun serialize(root: AccessibilityNodeInfo): SerializedNode` — recursively copies fields; uses `NodeHolder` for every child; checks `isPassword` before copying text (set `text = null` for password fields in the serialized tree — actual redaction happens in `getText()`)
  - `data class UiTreeSnapshot(val root: SerializedNode?, val timestamp: Long)`
  - Signature: takes an `AccessibilityNodeInfo` and returns a snapshot; does NOT hold the node after returning

- [ ] **Step 5: Write `DroidPilotAccessibilityService.kt`**

  Key behavior:
  - `companion object { @Volatile var instance: DroidPilotAccessibilityService? = null }`
  - In `onServiceConnected()`: set `instance = this`, take initial snapshot
  - In `onAccessibilityEvent(event)`: on `TYPE_WINDOW_CONTENT_CHANGED` or `TYPE_WINDOWS_CHANGED`, update snapshot; emit tick on `_eventFlow`
  - `_eventFlow: MutableSharedFlow<Unit>` with `replay = 1`; exposed as `val eventFlow: SharedFlow<Unit>`
  - In `onInterrupt()` and `onServiceDisconnected()`: clear `instance`
  - `fun updateSnapshot()`: calls `getRootInActiveWindow()`, wraps in `NodeHolder.of()`, calls `UiTreeSnapshot.serialize()`, updates `_snapshot: AtomicReference<UiTreeSnapshot>`
  - DO NOT hold any `AccessibilityNodeInfo` reference as a field

- [ ] **Step 6: Write `AccessibilityWaitStrategy.kt`**

  Implements `WaitStrategy`:
  - Subscribes to `DroidPilotAccessibilityService.instance?.eventFlow`
  - On each tick, re-evaluates `condition()`
  - Also polls every 100 ms as fallback (in case events are missed)
  - Uses `withTimeoutOrNull(timeoutMs)` as outer bound
  - Raises `BackendUnavailableError("Accessibility", "Enable DroidPilot accessibility service in Settings")` if `instance == null`

- [ ] **Step 7: Run test — verify it passes**

  Run: `./gradlew :backend:accessibility:test`
  Expected: PASS (3 new tests)

- [ ] **Step 8: Commit**

  ```
  git add backend/accessibility/
  git commit -m "feat(phase-1): AccessibilityService, UiTreeSnapshot, AccessibilityWaitStrategy"
  ```

---

## Task 3: SelectorEngine — all selector types + 5+ fixture tests

**Files:**
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/SelectorEngine.kt`
- Create: `backend/accessibility/src/test/kotlin/com/droidpilot/backend/accessibility/SelectorEngineTest.kt`

**Interfaces:**
- Consumes: `SerializedNode` (Task 1), `Selector` (Phase 0), `Bounds` (Phase 0), `ElementNotFoundError`, `AutomationException`
- Produces: `SelectorEngine.find(selector, root): SerializedNode` and `findAll(selector, root): List<SerializedNode>` (consumed by Tasks 4 and 5)

- [ ] **Step 1: Write failing tests in `SelectorEngineTest.kt`**

  Five fixture trees defined as helpers in the test file. Tests:

  ```kotlin
  // Fixture 1 — Login screen (shallow)
  @Test fun `ResourceId finds by short id`()   // "btnLogin" matches "com.example:id/btnLogin"
  @Test fun `Text finds exact match`()
  @Test fun `ClassName finds button`()
  @Test fun `Clickable(true) finds clickable nodes`()

  // Fixture 2 — Item list (RecyclerView + children)
  @Test fun `Index finds node by position in parent`()
  @Test fun `Instance finds nth global match`()
  @Test fun `TextContains finds substring`()
  @Test fun `TextMatches finds regex match`()

  // Fixture 3 — Nested containers
  @Test fun `Child finds node within parent scope`()
  @Test fun `Compound AND-matches multiple conditions`()

  // Fixture 4 — Multi-package tree
  @Test fun `Package matches by package name`()
  @Test fun `Description matches contentDescription`()
  @Test fun `Enabled(true) filters disabled nodes`()

  // Fixture 5 — Error paths
  @Test fun `find throws ElementNotFoundError when not found`()
  @Test fun `XPath throws UnsupportedOperationException`()

  // Fixture 6 — Sibling
  @Test fun `Sibling finds node adjacent to anchor`()
  ```

  Run: `./gradlew :backend:accessibility:test`
  Expected: FAIL — `SelectorEngine` not defined

- [ ] **Step 2: Implement `SelectorEngine.kt`**

  Object with:
  - `fun find(selector: Selector, root: SerializedNode): SerializedNode` — throws `ElementNotFoundError(selector)` if not found
  - `fun findAll(selector: Selector, root: SerializedNode): List<SerializedNode>` — returns empty list if not found (no throw)

  Internal traversal:
  - `private fun matches(node: SerializedNode, selector: Selector): Boolean` — handles all 15 subtypes; `XPath` throws `UnsupportedOperationException("XPath not supported in Phase 1")`
  - `ResourceId`: match either exact (`"com.example:id/foo"`) or short-form (`"foo"` — matches if `id.endsWith(":id/$short")`)
  - `Instance`: count global matches in DFS order; return the nth (0-based)
  - `Index`: match `node.indexInParent == index`
  - `Child(parent, child)`: find all nodes matching `parent`, then search each's children for `child`
  - `Sibling(anchor, sibling)`: find the anchor node's parent, search parent's children for `sibling` (excluding anchor itself)
  - `Compound`: all parts must match (AND semantics)

- [ ] **Step 3: Run test — verify it passes**

  Run: `./gradlew :backend:accessibility:test`
  Expected: PASS (all 16+ selector tests + 3 snapshot tests from Task 2)

- [ ] **Step 4: Commit**

  ```
  git add backend/accessibility/
  git commit -m "feat(phase-1): SelectorEngine resolves all selector types"
  ```

---

## Task 4: AccessibilityUiElement — element actions + wait

**Files:**
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/AccessibilityUiElement.kt`
- Test: Add `AccessibilityUiElementTest.kt` (unit tests for password masking and wait)

**Interfaces:**
- Consumes: `SelectorEngine` (Task 3), `UiTreeSnapshot` from service, `AccessibilityWaitStrategy` (Task 2), `NodeHolder` (Task 1)
- Produces: `AccessibilityUiElement` implements `UiElement` (consumed by Task 5 `findAll`, by `:script:python` in Phase 2)

- [ ] **Step 1: Write failing tests**

  ```kotlin
  class AccessibilityUiElementTest {
      @Test fun `getText returns REDACTED for password field`()
      @Test fun `exists returns false when selector not in snapshot`()
      @Test fun `wait throws TimeoutError when element never appears`()
  }
  ```
  Run: `./gradlew :backend:accessibility:test`
  Expected: FAIL

- [ ] **Step 2: Implement `AccessibilityUiElement.kt`**

  Constructor: `class AccessibilityUiElement(private val selector: Selector, private val service: DroidPilotAccessibilityService) : UiElement`

  Key method contracts:
  - `exists()` — resolves selector against current snapshot; returns true/false; no throw
  - `click()` — resolves selector → gets live node via `NodeHolder.require(service.findNode(selector))` → `use { node.info.performAction(ACTION_CLICK) }`; after action, waits for next window event
  - `longClick()` — same as click with `ACTION_LONG_CLICK`
  - `setText(text)` — `performAction(ACTION_SET_TEXT, bundle)` where bundle has `AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE`
  - `clearText()` — `setText("")`
  - `getText()` — resolve node; if `info.isPassword` return `"[REDACTED]"`; otherwise return `info.text?.toString()`
  - `bounds()` — resolve node; copy bounds to `Bounds` data class; recycle
  - `info()` — resolve node; build `ElementInfo` from node fields; recycle
  - `wait(timeoutMs)` — use `AccessibilityWaitStrategy.await(timeoutMs) { exists() }`; if false throw `TimeoutError(selector, timeoutMs)`; return `this`
  - `waitGone(timeoutMs)` — `AccessibilityWaitStrategy.await(timeoutMs) { !exists() }`
  - `scrollTo(selector)` — perform `ACTION_SCROLL_FORWARD` on parent scrollable; create a new `AccessibilityUiElement` for the child
  - `child(selector)` / `sibling(selector)` — return new `AccessibilityUiElement` with `Selector.Child`/`Selector.Sibling` wrapping the current selector

  Add `fun findNode(selector: Selector): AccessibilityNodeInfo?` to `DroidPilotAccessibilityService` — resolves selector against snapshot, then walks the live tree to find the matching node by path; wraps returned node in caller-owned `NodeHolder` (caller must recycle).

- [ ] **Step 3: Run tests**

  Run: `./gradlew :backend:accessibility:test`
  Expected: PASS

- [ ] **Step 4: Commit**

  ```
  git add backend/accessibility/
  git commit -m "feat(phase-1): AccessibilityUiElement — click, setText, getText, wait"
  ```

---

## Task 5: AccessibilityDeviceActions — gestures + Hilt wiring

**Files:**
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/AccessibilityDeviceActions.kt`
- Create: `backend/accessibility/src/main/kotlin/com/droidpilot/backend/accessibility/di/AccessibilityModule.kt`
- Modify: `app/src/main/kotlin/com/droidpilot/app/di/AppModule.kt` — bind `DeviceActions` to `AccessibilityDeviceActions` via Hilt
- Test: `AccessibilityDeviceActionsTest.kt` (unit tests for unsupported action errors)

**Interfaces:**
- Consumes: `DroidPilotAccessibilityService` (Task 2), `SelectorEngine` (Task 3), `AccessibilityUiElement` (Task 4)
- Produces: `AccessibilityDeviceActions implements DeviceActions` — the Phase 1 backend; injected as `@Singleton` via Hilt

- [ ] **Step 1: Write failing test**

  ```kotlin
  class AccessibilityDeviceActionsTest {
      @Test fun `shell throws BackendUnavailableError`()
      @Test fun `appClear throws BackendUnavailableError`()
      @Test fun `find returns AccessibilityUiElement`()
  }
  ```
  Run: `./gradlew :backend:accessibility:test`
  Expected: FAIL

- [ ] **Step 2: Implement `AccessibilityDeviceActions.kt`**

  `@Singleton class AccessibilityDeviceActions @Inject constructor() : DeviceActions`

  Gesture methods — all use `dispatchGesture(GestureDescription, callback, handler)`:
  - `tap(x, y)` — single stroke from `(x,y)` to `(x,y)`, duration 100 ms
  - `swipe(startX, startY, endX, endY, durationMs)` — single stroke
  - `drag(startX, startY, endX, endY, durationMs)` — single stroke (same as swipe; drag semantics depend on app)
  - `pinch(centerX, centerY, scale)` — two simultaneous strokes: one from center outward, one inward; scale > 1 = zoom in, < 1 = zoom out

  All gesture dispatches: wrap in `suspendCoroutine { cont -> dispatchGesture(..., callback, null) }` where callback resumes the continuation.

  Key navigation:
  - `press(BACK)` → `performGlobalAction(GLOBAL_ACTION_BACK)`
  - `press(HOME)` → `performGlobalAction(GLOBAL_ACTION_HOME)`
  - `press(RECENTS)` → `performGlobalAction(GLOBAL_ACTION_RECENTS)`
  - `openNotifications()` → `performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)`

  Info methods:
  - `windowSize()` → if API >= 30: `windowManager.currentWindowMetrics.bounds`; else `displayMetrics`
  - `currentApp()` → `service.rootInActiveWindow?.packageName` (use `NodeHolder`, recycle immediately)
  - `screenshot()` → if API >= 30: `service.takeScreenshot(...)`; else throw `BackendUnavailableError("Accessibility", "Screenshot requires Android 11+ or Shizuku (Phase 6)")`

  Shell/app control (unsupported at this phase):
  - `shell()`, `appClear()`, `appInstall()` → throw `BackendUnavailableError("Accessibility", "Requires Shizuku (Phase 6)")`
  - `appStart(pkg, activity)` → start via `Intent(Intent.ACTION_MAIN).setPackage(pkg)` using `context.startActivity` (best-effort; works for launchable packages)
  - `appStop(pkg)` → throw `BackendUnavailableError("Accessibility", "Requires Shizuku (Phase 6)")`

  Element lookup:
  - `find(selector)` → `AccessibilityUiElement(selector, service)` (lazy — resolves on first action)
  - `findAll(selector)` → `SelectorEngine.findAll(selector, snapshot.root ?: return emptyList()).map { AccessibilityUiElement(selector, service) }` — returns one element per match

- [ ] **Step 3: Write `di/AccessibilityModule.kt`**

  ```kotlin
  @Module @InstallIn(SingletonComponent::class)
  object AccessibilityModule {
      @Provides @Singleton
      fun provideDeviceActions(impl: AccessibilityDeviceActions): DeviceActions = impl
  }
  ```

  Add `implementation(project(":backend:accessibility"))` to `:app/build.gradle.kts`.

- [ ] **Step 4: Run unit tests**

  Run: `./gradlew :backend:accessibility:test :app:test`
  Expected: PASS

- [ ] **Step 5: Commit**

  ```
  git add backend/accessibility/ app/
  git commit -m "feat(phase-1): AccessibilityDeviceActions gestures, find/findAll, Hilt wiring"
  ```

---

## Task 6: Detekt rule + instrumented end-to-end test

**Files:**
- Create: `detekt-rules/build.gradle.kts`
- Create: `detekt-rules/src/main/kotlin/com/droidpilot/detekt/AccessibilityNodeInfoLeakRule.kt`
- Create: `detekt-rules/src/main/kotlin/com/droidpilot/detekt/DroidPilotRuleSetProvider.kt`
- Create: `detekt-rules/src/main/resources/META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider`
- Modify: `settings.gradle.kts` — include `:detekt-rules`
- Modify: `build.gradle.kts` (root) — apply Detekt plugin, configure with custom rules JAR
- Create: `config/detekt/detekt.yml` — enable `AccessibilityNodeInfoLeak` rule
- Create: `backend/accessibility/src/androidTest/kotlin/com/droidpilot/backend/accessibility/LoginFlowTest.kt`

**Interfaces:**
- Consumes: `AccessibilityDeviceActions` (Task 5), `:sample-target` APK (Phase 0)
- Produces: `./gradlew detekt` passes with no `AccessibilityNodeInfoLeak` violations; `./gradlew connectedDebugAndroidTest` passes the login flow test

- [ ] **Step 1: Look up current stable Detekt version**

  Check: https://github.com/detekt/detekt/releases  
  Record the exact version before writing any file.

- [ ] **Step 2: Write failing Detekt check**

  Add a temporary file `backend/accessibility/src/main/kotlin/.../Leaky.kt` with:
  ```kotlin
  private val leakedNode: AccessibilityNodeInfo? = null  // intentional leak for test
  ```
  Run: `./gradlew :backend:accessibility:detekt` (Detekt not yet configured — will fail with "task not found" or with no error)
  Expected: the leak is NOT caught yet — confirms the rule is needed.

- [ ] **Step 3: Create `detekt-rules` module**

  `detekt-rules/build.gradle.kts`:
  ```kotlin
  plugins { kotlin("jvm") }
  dependencies {
      compileOnly("io.gitlab.arturbosch.detekt:detekt-api:<verified-version>")
      testImplementation("io.gitlab.arturbosch.detekt:detekt-test:<verified-version>")
      testImplementation(libs.junit)
  }
  ```
  Add `:detekt-rules` to `settings.gradle.kts`. Add `kotlin("jvm")` plugin alias to version catalog if not present.

- [ ] **Step 4: Write `AccessibilityNodeInfoLeakRule.kt`**

  Extends `io.gitlab.arturbosch.detekt.api.Rule`. Visits `KtProperty` nodes: flags any non-local `var` or `val` property whose declared type is `AccessibilityNodeInfo` and that is not inside a class named `NodeHolder`. Message: `"AccessibilityNodeInfo must be held only in NodeHolder — holding it as a field causes a leak"`.

- [ ] **Step 5: Write `DroidPilotRuleSetProvider.kt` and service file**

  ```kotlin
  class DroidPilotRuleSetProvider : RuleSetProvider {
      override val ruleSetId = RuleSetId("droidpilot")
      override fun instance() = RuleSet(ruleSetId, listOf(::AccessibilityNodeInfoLeakRule))
  }
  ```
  Service file content: `com.droidpilot.detekt.DroidPilotRuleSetProvider`

- [ ] **Step 6: Configure Detekt in root `build.gradle.kts`**

  Apply `io.gitlab.arturbosch.detekt` plugin with `apply false`, then configure in a `subprojects` block:
  ```kotlin
  subprojects {
      apply(plugin = "io.gitlab.arturbosch.detekt")
      dependencies {
          "detektPlugins"(project(":detekt-rules"))
      }
  }
  ```
  Create `config/detekt/detekt.yml` enabling the `AccessibilityNodeInfoLeak` rule:
  ```yaml
  droidpilot:
    AccessibilityNodeInfoLeak:
      active: true
  ```

- [ ] **Step 7: Verify rule catches the intentional leak**

  Run: `./gradlew :backend:accessibility:detekt`
  Expected: FAIL — reports `AccessibilityNodeInfoLeak` on `Leaky.kt`
  Then delete `Leaky.kt` and run again.
  Expected: PASS — no violations

- [ ] **Step 8: Write `LoginFlowTest.kt`**

  Instrumented test that assumes:
  - DroidPilot accessibility service is already enabled (set up in CI/device)
  - `:sample-target` is already installed

  ```kotlin
  @HiltAndroidTest
  class LoginFlowTest {
      @Test
      fun `login flow completes successfully`() {
          val device = AccessibilityDeviceActions()  // or inject via Hilt test
          // Start sample-target
          device.appStart("com.droidpilot.sampletarget")
          // Fill username
          device.find(Selector.ResourceId("username"))
              .wait(10_000)
              .setText("qa_user")
          // Fill password
          device.find(Selector.ResourceId("password"))
              .setText("secret")
          // Click login
          device.find(Selector.ResourceId("btnLogin")).click()
          // Verify item list visible
          assertTrue(device.find(Selector.ResourceId("rvItems")).wait(10_000).exists())
      }
  }
  ```

- [ ] **Step 9: Update CLAUDE.md Phase 1 checklist**

  Mark all Phase 1 items complete.

- [ ] **Step 10: Full verification**

  Run: `./gradlew assembleDebug test detekt`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

  ```
  git add detekt-rules/ config/ settings.gradle.kts build.gradle.kts backend/accessibility/ CLAUDE.md
  git commit -m "feat(phase-1): Detekt AccessibilityNodeInfoLeak rule + E2E login flow test"
  ```

---

## Self-Review Notes

**Spec coverage:**
- `AccessibilityService` declared + config: Task 2 ✓
- UI tree snapshot on window events: Task 2 ✓
- `SelectorEngine` all `Selector` types: Task 3 ✓
- `UiElement` actions click/longClick/setText/getText/exists/bounds: Task 4 ✓
- `WaitStrategy` event-driven + polling fallback: Tasks 1 + 2 ✓
- Gesture dispatch tap/swipe/drag/pinch: Task 5 ✓
- Unit tests 5+ serialized fixtures: Task 3 ✓ (6 fixtures, 16+ tests)
- Instrumented test drives sample-target login: Task 6 ✓
- `AccessibilityNodeInfo` leak Detekt rule: Task 6 ✓

**Review Focus addressed:**
1. NodeHolder closure race → documented in Task 4 Step 2 (action must complete inside `use {}`)
2. WaitStrategy event race → Task 2 Step 6 uses `replay = 1` SharedFlow; subscribe before check
3. Index vs Instance semantics → separate test fixtures (Fixture 1 for Index, Fixture 2 for Instance)
4. XPath throws `UnsupportedOperationException` → Fixture 5 test exercises the throw
5. Instrumented test timeout → all `wait()` calls use `10_000` ms

**Proportion check:** No full function bodies in the plan except for small data classes and interfaces where the exact signatures matter. Implementation judgment calls are left to the implementer.
