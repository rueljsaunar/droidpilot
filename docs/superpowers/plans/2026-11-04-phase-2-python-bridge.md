# Phase 2 – Python Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire Chaquopy 17.2.0 (Python 3.10) into `:script:python` so a Python test script can drive `:sample-target` through the Phase 1 accessibility backend.

**Architecture:** Python scripts run in Chaquopy's embedded interpreter; they import `droidpilot`, whose `Device` class calls a Kotlin singleton `DeviceProxy` via Chaquopy's Java bridge; `DeviceProxy` wraps every `DeviceActions` suspend call with `runBlocking` (safe because Python runs on a background thread). `ScriptRunner` is the Kotlin entry point that starts execution of a script string.

**Tech Stack:** Chaquopy 17.2.0, Python 3.10, Kotlin coroutines (`runBlocking`), Hilt `@EntryPoint`, JUnit 4.

**Spec:** `CLAUDE.md` § Phase 2 checklist; architecture in `docs/ARCHITECTURE.md`.

## Global Constraints

- Chaquopy 17.2.0 — plugin ID `com.chaquo.python`, verified at https://chaquo.com/chaquopy/doc/current/changelog.html
- Python 3.10 — minimum supported by Chaquopy 17.x (3.8 dropped in 17.0)
- ABI filters: `arm64-v8a`, `x86_64` — covers physical + emulator targets
- No Android framework imports in `:core:automation`
- No password text in logs — `isPassword` check before any text logging
- No hard sleeps — all waits through Phase 1 `WaitStrategy`
- UI must look modern — Material 3, dynamic color, edge-to-edge (applies to any UI added this phase; none expected)
- Selector encoding format: `"<type>:<value>"` string passed across the Python↔Kotlin boundary
- `DeviceProxy` is a Kotlin `object`; its `DeviceActions` reference is obtained via Hilt `@EntryPoint` from the application context — no lateinit or manual injection in Hilt-managed components

## Review Focus

- **Script thread vs main thread**: `runBlocking` in `DeviceProxy` blocks the Python (background) thread; it must never be called from the main thread. Add an assertion `check(!Looper.getMainLooper().isCurrentThread)` at the start of every `runBlocking` call in `DeviceProxy` so the error surfaces clearly instead of ANR.
- **Python interpreter not started**: if `Python.start()` was not called before `ScriptRunner.run()`, Chaquopy throws a `PyException`. `ScriptRunner` must catch this and rethrow as `IllegalStateException("Python interpreter not started — call Python.start() first")`.
- **AutomationException propagation**: Kotlin exceptions thrown inside `runBlocking` cross the Chaquopy bridge as `java.lang.RuntimeException` wrapping the original. `_element.py` and `_device.py` must catch `java.lang.RuntimeException`, inspect the cause's class name, and re-raise the appropriate `DroidPilotError` subclass.
- **`wait()` returning self**: `UiElementProxy.wait()` returns `self` (via `return this`) so Python chained calls like `d.find(...).wait(5000).set_text("x")` work without intermediate variables.
- **Selector encoding delimiter collision**: if a resource ID or text value contains `:`, the `"<type>:<value>"` encoding produces the wrong split. `parseSelector` must split on only the FIRST colon (`encoded.indexOf(':')` → `encoded.substring(0, colon)` / `encoded.substring(colon + 1)`). Add a unit test with a resource ID that contains a colon.

---

## Task 1: Chaquopy Gradle Configuration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `script/python/build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/kotlin/com/droidpilot/app/DroidPilotApp.kt`

**Interfaces:**
- Produces: `Python.getInstance()` available at runtime; `src/main/python/` packaged into `:script:python` AAR

- [ ] **Step 1: Add Chaquopy to `libs.versions.toml`**

  Add to `[versions]`:
  ```toml
  chaquopy = "17.2.0"
  ```
  Add to `[plugins]`:
  ```toml
  chaquopy = { id = "com.chaquo.python", version.ref = "chaquopy" }
  ```

- [ ] **Step 2: Apply Chaquopy plugin to `:script:python`**

  Replace `script/python/build.gradle.kts` with:
  ```kotlin
  plugins {
      alias(libs.plugins.android.library)
      alias(libs.plugins.chaquopy)
  }
  android {
      namespace = "com.droidpilot.script.python"
      compileSdk = libs.versions.compileSdk.get().toInt()
      defaultConfig {
          minSdk = libs.versions.minSdk.get().toInt()
          ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
          python { version = "3.10" }
      }
      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_17
          targetCompatibility = JavaVersion.VERSION_17
      }
  }
  dependencies {
      implementation(project(":core:automation"))
      implementation(project(":backend:accessibility"))
      testImplementation(libs.junit)
      testImplementation(libs.coroutines.test)
      androidTestImplementation(libs.androidx.test.runner)
      androidTestImplementation(libs.androidx.test.ext.junit)
  }
  ```

- [ ] **Step 3: Apply Chaquopy plugin to `:app`** (required for library module Python initialisation)

  Add `alias(libs.plugins.chaquopy)` to the plugins block in `app/build.gradle.kts`. No Python source files; no additional config block needed beyond what the plugin infers.

- [ ] **Step 4: Start Python and init DeviceProxy in `DroidPilotApp.onCreate()`**

  ```kotlin
  import com.chaquo.python.android.AndroidPlatform
  import com.chaquo.python.Python
  import com.droidpilot.script.python.DeviceProxy

  override fun onCreate() {
      super.onCreate()
      if (!Python.isStarted()) {
          Python.start(AndroidPlatform(this))
      }
      DeviceProxy.init(this)   // wires Hilt-managed DeviceActions into the proxy
  }
  ```

- [ ] **Step 5: Sync and verify build**

  Run: `./gradlew :script:python:assembleDebug :app:assembleDebug`
  Expected: BUILD SUCCESSFUL; no "unresolved chaquopy" errors.

- [ ] **Step 6: Commit**

  ```
  git add gradle/libs.versions.toml script/python/build.gradle.kts \
         app/build.gradle.kts app/src/main/kotlin/com/droidpilot/app/DroidPilotApp.kt
  git commit -m "build: add Chaquopy 17.2.0, Python 3.10 to :script:python and :app"
  ```

---

## Task 2: DeviceProxy & UiElementProxy (Kotlin Bridge)

**Files:**
- Create: `script/python/src/main/kotlin/com/droidpilot/script/python/DeviceProxy.kt`
- Create: `script/python/src/main/kotlin/com/droidpilot/script/python/UiElementProxy.kt`
- Create: `script/python/src/test/kotlin/com/droidpilot/script/python/DeviceProxyTest.kt`

**Interfaces:**
- Consumes: `DeviceActions`, `UiElement`, `Selector`, `Bounds`, `ElementInfo` from `:core:automation`; `AccessibilityDeviceActions` for Hilt binding
- Produces:
  - `DeviceProxy.init(context: Context)` — call once from `DroidPilotApp`
  - `DeviceProxy.find(selectorEncoded: String): UiElementProxy`
  - `DeviceProxy.findAll(selectorEncoded: String): List<UiElementProxy>`
  - `DeviceProxy.tap(x: Float, y: Float)`
  - `DeviceProxy.swipe(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long)`
  - `DeviceProxy.drag(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long)`
  - `DeviceProxy.pinch(cx: Float, cy: Float, scale: Float)`
  - `DeviceProxy.press(key: String)` — `"BACK"`, `"HOME"`, `"RECENTS"`
  - `DeviceProxy.appStart(pkg: String, activity: String?)`
  - `DeviceProxy.windowSize(): IntArray` — `[width, height]`
  - `DeviceProxy.currentApp(): String?`
  - `UiElementProxy.click()`, `.longClick()`, `.setText(text)`, `.clearText()`, `.getText(): String?`
  - `UiElementProxy.exists(): Boolean`, `.wait(timeoutMs: Long): UiElementProxy`, `.waitGone(timeoutMs: Long): Boolean`
  - `UiElementProxy.info(): Map<String, Any?>`

- [ ] **Step 1: Write failing unit test**

  ```kotlin
  class DeviceProxyTest {
      private val fakeActions = FakeDeviceActions()

      @Before fun setUp() { DeviceProxy.initForTest(fakeActions) }

      @Test fun `find returns proxy that delegates click`() = runBlocking {
          DeviceProxy.find("id:username").click()
          assertTrue(fakeActions.lastClicked == Selector.ResourceId("username"))
      }

      @Test fun `find with colon in resource id parses correctly`() = runBlocking {
          DeviceProxy.find("id:com.example:id/button").exists()
          assertTrue(fakeActions.lastFound == Selector.ResourceId("com.example:id/button"))
      }

      @Test fun `tap delegates x y to actions`() = runBlocking {
          DeviceProxy.tap(100f, 200f)
          assertEquals(100f to 200f, fakeActions.lastTap)
      }

      @Test fun `press HOME delegates correct key`() = runBlocking {
          DeviceProxy.press("HOME")
          assertEquals(DeviceKey.HOME, fakeActions.lastKey)
      }
  }
  ```

  `FakeDeviceActions` records calls; `initForTest(actions)` bypasses Hilt.

- [ ] **Step 2: Run failing test**

  Run: `./gradlew :script:python:test --tests "*.DeviceProxyTest"`
  Expected: FAIL — `DeviceProxy` not yet defined.

- [ ] **Step 3: Implement `UiElementProxy`**

  `UiElementProxy(private val selector: Selector, private val actions: DeviceActions)`:
  - Each method body wraps the corresponding `actions.find(selector).*` suspend call in `runBlocking { check(!isMainThread()); … }`
  - `wait(timeoutMs)` calls `actions.find(selector).wait(timeoutMs)` and returns `this`
  - `info()` returns `mapOf("text" to …, "description" to …, "resourceId" to …, "enabled" to …, "password" to …, "clickable" to …)`

- [ ] **Step 4: Implement `DeviceProxy`**

  `object DeviceProxy`:
  - `@EntryPoint @InstallIn(SingletonComponent::class) interface Entry { fun deviceActions(): DeviceActions }`
  - `private var appContext: Context? = null`
  - `private var testActions: DeviceActions? = null`
  - `internal fun initForTest(a: DeviceActions) { testActions = a }` (test-only)
  - `fun init(ctx: Context) { appContext = ctx.applicationContext }`
  - `private val actions get() = testActions ?: EntryPointAccessors.fromApplication(appContext!!, Entry::class.java).deviceActions()`
  - `fun find(enc: String): UiElementProxy = UiElementProxy(parseSelector(enc), actions)`
  - `fun findAll(enc: String): List<UiElementProxy>` — delegates to `actions.findAll(parseSelector(enc))`
  - `fun tap(x, y)` etc. — each wraps `runBlocking { check(!isMainThread()); actions.tap(x, y) }`
  - `private fun parseSelector(enc: String): Selector` — splits on first `:`, maps type string to `Selector.*`
  - `private fun isMainThread() = Looper.getMainLooper().isCurrentThread`

- [ ] **Step 5: Run test to verify it passes**

  Run: `./gradlew :script:python:test --tests "*.DeviceProxyTest"`
  Expected: PASS 4/4.

- [ ] **Step 6: Commit**

  ```
  git add script/python/src/main/kotlin/com/droidpilot/script/python/DeviceProxy.kt \
         script/python/src/main/kotlin/com/droidpilot/script/python/UiElementProxy.kt \
         script/python/src/test/kotlin/com/droidpilot/script/python/DeviceProxyTest.kt
  git commit -m "feat(phase-2): DeviceProxy + UiElementProxy Kotlin bridge"
  ```

---

## Task 3: `droidpilot` Python Package

**Files:**
- Create: `script/python/src/main/python/droidpilot/__init__.py`
- Create: `script/python/src/main/python/droidpilot/_errors.py`
- Create: `script/python/src/main/python/droidpilot/_selector.py`
- Create: `script/python/src/main/python/droidpilot/_element.py`
- Create: `script/python/src/main/python/droidpilot/_device.py`
- Create: `script/python/src/main/python/droidpilot/_expect.py`

**Interfaces:**
- Consumes: `com.droidpilot.script.python.DeviceProxy` and `UiElementProxy` via Chaquopy Java bridge
- Produces (Python public API):
  - `dp.Device()` — returns `Device` instance
  - `dp.id(value)`, `dp.text(value)`, `dp.text_contains(value)`, `dp.text_matches(pattern)`, `dp.desc(value)`, `dp.xpath(expr)`, `dp.cls(name)`, `dp.pkg(name)` — return encoded selector strings
  - `dp.HOME`, `dp.BACK`, `dp.RECENTS` — string key constants
  - `dp.expect(element)` — returns `Expectation`
  - `dp.DroidPilotError`, `dp.ElementNotFoundError`, `dp.TimeoutError`

- [ ] **Step 1: Write `_errors.py`**

  ```python
  class DroidPilotError(Exception):
      """Base class for all droidpilot automation errors."""

  class ElementNotFoundError(DroidPilotError):
      """Raised when a selector matches no element within the timeout."""

  class TimeoutError(DroidPilotError):
      """Raised when a wait condition is not met within the timeout."""

  class AssertionError(DroidPilotError):
      """Raised when an expect() assertion fails."""
  ```

- [ ] **Step 2: Write `_selector.py`**

  Eight functions — each accepts its argument and returns `"<type>:<value>"`:
  ```python
  def id(value): return f"id:{value}"
  def text(value): return f"text:{value}"
  def text_contains(value): return f"text_contains:{value}"
  def text_matches(pattern): return f"text_matches:{pattern}"
  def desc(value): return f"desc:{value}"
  def xpath(expr): return f"xpath:{expr}"
  def cls(name): return f"class:{name}"
  def pkg(name): return f"pkg:{name}"
  ```

- [ ] **Step 3: Write `_element.py`**

  `Element` wraps a `UiElementProxy` Java object:
  ```python
  class Element:
      def __init__(self, proxy):
          self._proxy = proxy

      def click(self): self._proxy.click(); return self
      def long_click(self): self._proxy.longClick(); return self
      def set_text(self, text): self._proxy.setText(text); return self
      def clear_text(self): self._proxy.clearText(); return self
      def get_text(self): return self._proxy.getText()
      def exists(self): return self._proxy.exists()
      def wait(self, timeout=5000): self._proxy.wait(timeout); return self
      def wait_gone(self, timeout=5000): return self._proxy.waitGone(timeout)
      def info(self): return dict(self._proxy.info())
  ```

  Catch `java.lang.RuntimeException` in each method; inspect `.getCause()` class name; re-raise `ElementNotFoundError` for `ElementNotFoundError`, `TimeoutError` for `TimeoutError`, `DroidPilotError` for all others.

- [ ] **Step 4: Write `_device.py`**

  ```python
  from com.droidpilot.script.python import DeviceProxy as _Proxy
  from ._element import Element

  class Device:
      """Entry point for all automation actions."""

      def find(self, selector): return Element(_Proxy.find(selector))
      def find_all(self, selector): return [Element(p) for p in _Proxy.findAll(selector)]
      def tap(self, x, y): _Proxy.tap(float(x), float(y))
      def swipe(self, sx, sy, ex, ey, duration_ms=300):
          _Proxy.swipe(float(sx), float(sy), float(ex), float(ey), duration_ms)
      def drag(self, sx, sy, ex, ey, duration_ms=500):
          _Proxy.drag(float(sx), float(sy), float(ex), float(ey), duration_ms)
      def pinch(self, cx, cy, scale): _Proxy.pinch(float(cx), float(cy), float(scale))
      def press(self, key): _Proxy.press(str(key))
      def app_start(self, pkg, activity=None): _Proxy.appStart(pkg, activity)
      def window_size(self):
          arr = _Proxy.windowSize(); return (arr[0], arr[1])
      def current_app(self): return _Proxy.currentApp()
  ```

- [ ] **Step 5: Write `_expect.py`**

  ```python
  from ._errors import AssertionError, TimeoutError as _TimeoutError

  class Expectation:
      def __init__(self, element): self._element = element

      def to_be_visible(self, timeout=5000):
          try:
              self._element.wait(timeout)
          except _TimeoutError as e:
              raise AssertionError(f"Expected element to be visible within {timeout}ms") from e
          if not self._element.exists():
              raise AssertionError("Expected element to be visible but exists() returned False")
          return self

      def to_be_gone(self, timeout=5000):
          if not self._element.wait_gone(timeout):
              raise AssertionError(f"Expected element to be gone within {timeout}ms")
          return self

      def to_have_text(self, expected):
          actual = self._element.get_text()
          if actual != expected:
              raise AssertionError(f"Expected text '{expected}', got '{actual}'")
          return self

      def to_be_enabled(self):
          if not self._element.info().get("enabled"):
              raise AssertionError("Expected element to be enabled")
          return self

  def expect(element): return Expectation(element)
  ```

- [ ] **Step 6: Write `__init__.py`**

  ```python
  from ._device import Device
  from ._selector import id, text, text_contains, text_matches, desc, xpath, cls, pkg
  from ._expect import expect
  from ._errors import DroidPilotError, ElementNotFoundError, TimeoutError, AssertionError

  HOME = "HOME"
  BACK = "BACK"
  RECENTS = "RECENTS"

  __all__ = [
      "Device",
      "id", "text", "text_contains", "text_matches", "desc", "xpath", "cls", "pkg",
      "expect",
      "HOME", "BACK", "RECENTS",
      "DroidPilotError", "ElementNotFoundError", "TimeoutError", "AssertionError",
  ]
  ```

- [ ] **Step 7: Verify build still passes**

  Run: `./gradlew :script:python:assembleDebug`
  Expected: BUILD SUCCESSFUL (Python files packaged into AAR).

- [ ] **Step 8: Commit**

  ```
  git add script/python/src/main/python/
  git commit -m "feat(phase-2): droidpilot Python package — Device, selectors, expect()"
  ```

---

## Task 4: ScriptRunner + End-to-End Instrumented Test

**Files:**
- Create: `script/python/src/main/kotlin/com/droidpilot/script/python/ScriptRunner.kt`
- Create: `script/python/src/main/python/tests/test_sample_target.py`
- Create: `script/python/src/androidTest/kotlin/com/droidpilot/script/python/PythonBridgeTest.kt`

**Interfaces:**
- Consumes: `DeviceProxy.init(context)`, `Python.getInstance()`, `droidpilot` package
- Produces:
  - `ScriptRunner.run(script: String)` — executes `script` in the Python interpreter; throws `IllegalStateException` if Python not started, rethrows Python exceptions wrapped in `RuntimeException`

- [ ] **Step 1: Write failing instrumented test**

  ```kotlin
  // No @HiltAndroidTest needed — DroidPilotApp.onCreate() already called
  // Python.start() and DeviceProxy.init() before any test runs.
  @RunWith(AndroidJUnit4::class)
  class PythonBridgeTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

      @Test fun `hardcoded login script drives sample-target end to end`() {
          val script = ctx.assets.open("tests/test_sample_target.py")
              .bufferedReader().readText()
          ScriptRunner.run(script)
          // passes only if no exception is thrown; assertions live inside the Python script
      }
  }
  ```

- [ ] **Step 2: Write `tests/test_sample_target.py`**

  Place at `script/python/src/main/assets/tests/test_sample_target.py` (packaged as an asset so the instrumented test can load it via `context.assets`):

  ```python
  import droidpilot as dp

  d = dp.Device()
  d.app_start("com.droidpilot.sampletarget")

  d.find(dp.id("username")).wait(timeout=10000).set_text("qa_user")
  d.find(dp.id("password")).set_text("secret")
  d.find(dp.id("btnLogin")).click()

  dp.expect(d.find(dp.id("rvItems"))).to_be_visible(timeout=10000)
  ```

- [ ] **Step 3: Run failing instrumented test**

  Run: `./gradlew :script:python:connectedDebugAndroidTest --tests "*.PythonBridgeTest"`
  Expected: FAIL — `ScriptRunner` not yet defined.

- [ ] **Step 4: Implement `ScriptRunner`**

  ```kotlin
  object ScriptRunner {
      fun run(script: String) {
          check(Python.isStarted()) {
              "Python interpreter not started — call Python.start() first"
          }
          val interp = Python.getInstance().getModule("builtins").callAttr("compile",
              script, "<droidpilot>", "exec")
          // exec via __builtins__
          Python.getInstance().getBuiltins().callAttr("exec", interp)
      }
  }
  ```

  Simpler alternative using `getInterpreter().exec(script)` if the Chaquopy version exposes it; fall back to the `builtins.exec` approach above.

- [ ] **Step 5: Run instrumented test to verify it passes**

  Prerequisites: accessibility service enabled; `:sample-target` installed.
  Run: `./gradlew :script:python:connectedDebugAndroidTest --tests "*.PythonBridgeTest"`
  Expected: PASS 1/1.

- [ ] **Step 6: Run full unit test suite**

  Run: `./gradlew test`
  Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 7: Commit**

  ```
  git add script/python/src/main/kotlin/com/droidpilot/script/python/ScriptRunner.kt \
         script/python/src/main/assets/tests/test_sample_target.py \
         script/python/src/androidTest/kotlin/com/droidpilot/script/python/PythonBridgeTest.kt
  git commit -m "feat(phase-2): ScriptRunner + E2E Python login test against :sample-target"
  ```
