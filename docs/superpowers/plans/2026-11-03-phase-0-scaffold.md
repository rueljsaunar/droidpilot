# Phase 0 – Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a compiling multi-module Android project with Hilt, Compose shell, all module stubs, and a functional `:sample-target` demo app — no production logic yet, just the skeleton that every later phase builds on.

**Architecture:** Kotlin DSL Gradle with a single version catalog (`libs.versions.toml`). All modules are declared in `settings.gradle.kts`. `:core:automation` defines the sealed `Selector` hierarchy and key interfaces that later backend modules will implement. `:app` wires Hilt and hosts the Compose navigation graph with a single placeholder screen. `:sample-target` is a separate, independently installable APK used as the app-under-test.

**Tech Stack:** Kotlin 2.x, Android Gradle Plugin (latest stable), Jetpack Compose (BOM), Hilt, Navigation Compose, Material 3

**Spec:** [docs/ARCHITECTURE.md](../../ARCHITECTURE.md)

---

## Global Constraints

- `minSdk 26`, `targetSdk` = latest stable Android SDK (verify at implementation time)
- Kotlin 2.x; explicit API mode on all non-`:app` modules
- All dependency versions in `gradle/libs.versions.toml` only — no inline version literals in any `build.gradle.kts`
- Kotlin DSL exclusively — no Groovy `.gradle` files
- Base package: `com.droidpilot` (main app), `com.droidpilot.sampletarget` (sample target)
- No network permissions in any module at this phase
- `./gradlew assembleDebug` and `./gradlew test` must both pass before the phase-0 commit
- Lint: ktlint and detekt configured and passing (warnings allowed; errors block build)

---

## Review Focus

1. **Compose + Kotlin version mismatch** — Compose compiler requires a specific Kotlin version; a mismatch produces a cryptic annotation processor error, not a version message. Test: `./gradlew :app:compileDebugKotlin` succeeds with no annotation processor errors. Add to Task 1 verification step.
2. **Hilt missing @AndroidEntryPoint on Application subclass** — Hilt silently fails to inject until `@HiltAndroidApp` is present; the app still compiles and launches but crashes on first injection. Test: `DroidPilotApplicationTest` checks the class is annotated. Add to Task 4 verification.
3. **Circular module dependency** — a `build.gradle.kts` that creates a cycle (e.g., `:core:automation` depending on `:app`) causes a Gradle configuration error only at sync. Test: `./gradlew dependencies` produces no cycles. Add to Task 2 final step.
4. **`:sample-target` resource IDs inconsistent with Phase 1 test fixtures** — Phase 1 instrumented tests hard-code the IDs below; if they differ from the built APK the tests will fail silently (element not found). All IDs are pinned in Task 5.
5. **Gradle wrapper version incompatible with AGP** — an old wrapper with a new AGP produces a non-obvious "Unsupported class file major version" error. Test: `./gradlew --version` output matches the minimum wrapper version required by the chosen AGP. Add to Task 1 verification.

---

## Task 1: Gradle Foundation & Version Verification

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `.gitignore`

**Interfaces:**
- Produces: version aliases used by every subsequent task's `build.gradle.kts`

- [ ] **Step 1: Look up current stable versions**

  Open a browser or run web searches for each. Record exact versions before writing any file:
  - Android Gradle Plugin: https://developer.android.com/studio/releases/gradle-plugin
  - Kotlin: https://kotlinlang.org/docs/releases.html
  - Gradle wrapper (minimum required by chosen AGP): AGP release notes table
  - Compose BOM: https://developer.android.com/jetpack/compose/bom/bom-mapping (pick latest stable row)
  - Hilt: https://dagger.dev/hilt/ or Maven Central `com.google.dagger:hilt-android`
  - Coroutines: Maven Central `org.jetbrains.kotlinx:kotlinx-coroutines-android`
  - Navigation Compose: https://developer.android.com/jetpack/androidx/releases/navigation

- [ ] **Step 2: Write `gradle/libs.versions.toml`**

  Include these sections with the verified values from Step 1:

  ```toml
  [versions]
  agp = "<verified>"
  kotlin = "<verified>"
  compose-bom = "<verified>"
  hilt = "<verified>"
  coroutines = "<verified>"
  navigation = "<verified>"
  # (add more as needed per-phase)

  [libraries]
  androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "..." }
  compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
  compose-ui = { group = "androidx.compose.ui", name = "ui" }
  compose-material3 = { group = "androidx.compose.material3", name = "material3" }
  compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
  compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
  hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
  hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
  navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
  coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
  junit = { module = "junit:junit", version = "4.13.2" }

  [plugins]
  android-application = { id = "com.android.application", version.ref = "agp" }
  android-library = { id = "com.android.library", version.ref = "agp" }
  kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
  kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
  hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
  ```

- [ ] **Step 3: Write `settings.gradle.kts`**

  Declare the plugin management repos, dependency resolution management, and include all modules (they don't exist yet; Gradle will skip missing ones until Task 2):

  ```kotlin
  pluginManagement {
      repositories {
          google(); mavenCentral(); gradlePluginPortal()
      }
  }
  dependencyResolutionManagement {
      repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      repositories { google(); mavenCentral() }
      versionCatalogs { create("libs") { from(files("gradle/libs.versions.toml")) } }
  }
  rootProject.name = "DroidPilot"
  include(
      ":app",
      ":core:automation",
      ":backend:accessibility",
      ":backend:shizuku",
      ":backend:vision",
      ":script:python",
      ":runner",
      ":reporting",
      ":feature:editor",
      ":feature:projects",
      ":feature:overlay",
      ":feature:inspector",
      ":feature:recorder",
      ":sample-target",
  )
  ```

- [ ] **Step 4: Write root `build.gradle.kts`**

  ```kotlin
  plugins {
      alias(libs.plugins.android.application) apply false
      alias(libs.plugins.android.library) apply false
      alias(libs.plugins.kotlin.android) apply false
      alias(libs.plugins.hilt) apply false
  }
  ```

- [ ] **Step 5: Write `gradle/wrapper/gradle-wrapper.properties`**

  Set `distributionUrl` to the Gradle version required by the AGP version chosen in Step 1.

- [ ] **Step 6: Write `.gitignore`**

  Standard Android `.gitignore`: `.gradle/`, `build/`, `local.properties`, `*.iml`, `.idea/` (keep `.idea/codeStyles`).

- [ ] **Step 7: Verify Gradle wrapper compatibility**

  Run: `./gradlew --version`  
  Expected: prints Gradle version ≥ AGP minimum; no "Unsupported class file" errors.

- [ ] **Step 8: Commit**

  ```
  git add gradle/ settings.gradle.kts build.gradle.kts .gitignore
  git commit -m "feat(phase-0): gradle foundation and version catalog"
  ```

---

## Task 2: Module Stubs

**Files (create for each module listed):**
Each module at `<module-path>/build.gradle.kts` + a single placeholder Kotlin file so the module is non-empty.

Modules and their placeholder file paths:

| Module | build.gradle.kts type | Placeholder file |
|--------|----------------------|-----------------|
| `core/automation` | `android-library` | `src/main/kotlin/com/droidpilot/core/automation/Placeholder.kt` |
| `backend/accessibility` | `android-library` + `AndroidManifest.xml` | `src/main/kotlin/com/droidpilot/backend/accessibility/Placeholder.kt` |
| `backend/shizuku` | `android-library` | `src/main/kotlin/com/droidpilot/backend/shizuku/Placeholder.kt` |
| `backend/vision` | `android-library` | `src/main/kotlin/com/droidpilot/backend/vision/Placeholder.kt` |
| `script/python` | `android-library` | `src/main/kotlin/com/droidpilot/script/python/Placeholder.kt` |
| `runner` | `android-library` | `src/main/kotlin/com/droidpilot/runner/Placeholder.kt` |
| `reporting` | `android-library` | `src/main/kotlin/com/droidpilot/reporting/Placeholder.kt` |
| `feature/editor` | `android-library` | `src/main/kotlin/com/droidpilot/feature/editor/Placeholder.kt` |
| `feature/projects` | `android-library` | `src/main/kotlin/com/droidpilot/feature/projects/Placeholder.kt` |
| `feature/overlay` | `android-library` | `src/main/kotlin/com/droidpilot/feature/overlay/Placeholder.kt` |
| `feature/inspector` | `android-library` | `src/main/kotlin/com/droidpilot/feature/inspector/Placeholder.kt` |
| `feature/recorder` | `android-library` | `src/main/kotlin/com/droidpilot/feature/recorder/Placeholder.kt` |

**Interfaces:**
- Produces: compiling module stubs that `settings.gradle.kts` can resolve

- [ ] **Step 1: Write each library module's `build.gradle.kts`**

  Template (apply to all library modules, adjusting namespace):

  ```kotlin
  plugins {
      alias(libs.plugins.android.library)
      alias(libs.plugins.kotlin.android)
  }
  android {
      namespace = "com.droidpilot.<module-package>"
      compileSdk = libs.versions.compileSdk.get().toInt()  // add compileSdk to version catalog
      defaultConfig { minSdk = 26 }
      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_17
          targetCompatibility = JavaVersion.VERSION_17
      }
      kotlinOptions { jvmTarget = "17" }
  }
  ```

  Add `compileSdk` alias to `libs.versions.toml` at this step (latest stable SDK, verified).

  For `backend/accessibility`, also add a minimal `AndroidManifest.xml` (empty `<manifest>` with the module package; the actual service declaration comes in Phase 1).

- [ ] **Step 2: Write each placeholder Kotlin file**

  Content (same for all):

  ```kotlin
  package com.droidpilot.<module-package>
  // Phase 0 stub — implementation in later phases
  ```

- [ ] **Step 3: Verify no dependency cycles**

  Run: `./gradlew dependencies 2>&1 | grep -i cycle`  
  Expected: no output (no cycles detected).

- [ ] **Step 4: Verify all modules compile**

  Run: `./gradlew build`  
  Expected: BUILD SUCCESSFUL with no errors. Warnings about empty manifests are acceptable.

- [ ] **Step 5: Commit**

  ```
  git add core/ backend/ script/ runner/ reporting/ feature/
  git commit -m "feat(phase-0): add all module stubs"
  ```

---

## Task 3: `:core:automation` Interfaces

**Files:**
- Create: `core/automation/src/main/kotlin/com/droidpilot/core/automation/Selector.kt`
- Create: `core/automation/src/main/kotlin/com/droidpilot/core/automation/UiElement.kt`
- Create: `core/automation/src/main/kotlin/com/droidpilot/core/automation/DeviceActions.kt`
- Create: `core/automation/src/main/kotlin/com/droidpilot/core/automation/AutomationException.kt`
- Create: `core/automation/src/main/kotlin/com/droidpilot/core/automation/ElementInfo.kt`
- Create: `core/automation/src/main/kotlin/com/droidpilot/core/automation/ShellResult.kt`
- Delete: `core/automation/src/main/kotlin/com/droidpilot/core/automation/Placeholder.kt`
- Test: `core/automation/src/test/kotlin/com/droidpilot/core/automation/SelectorTest.kt`

**Interfaces:**
- Produces: `Selector`, `UiElement`, `DeviceActions`, `AutomationException` — used by all backend and feature modules from Phase 1 onward

- [ ] **Step 1: Write failing test in `SelectorTest.kt`**

  ```kotlin
  package com.droidpilot.core.automation

  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertTrue
  import org.junit.Test

  class SelectorTest {

      @Test
      fun `all selector subtypes instantiate`() {
          val selectors: List<Selector> = listOf(
              Selector.Text("Login"),
              Selector.TextContains("Log"),
              Selector.TextMatches("Log.*"),
              Selector.ResourceId("com.example:id/btn"),
              Selector.Description("Login button"),
              Selector.ClassName("android.widget.Button"),
              Selector.Package("com.example"),
              Selector.Clickable(true),
              Selector.Enabled(true),
              Selector.Index(0),
              Selector.Instance(1),
              Selector.XPath("//Button[@text='Login']"),
              Selector.Child(Selector.ClassName("LinearLayout"), Selector.Text("OK")),
              Selector.Sibling(Selector.Text("Label"), Selector.ClassName("EditText")),
              Selector.Compound(listOf(Selector.Text("Login"), Selector.Clickable(true))),
          )
          assertEquals(15, selectors.size)
      }

      @Test
      fun `Compound flattens nested compounds`() {
          val inner = Selector.Compound(listOf(Selector.Text("A"), Selector.Clickable(true)))
          val outer = Selector.Compound(listOf(inner, Selector.ClassName("Button")))
          val flat = outer.flatten()
          assertEquals(3, flat.size)
          assertTrue(flat.none { it is Selector.Compound })
      }

      @Test
      fun `AutomationException subtypes carry messages`() {
          val e1 = ElementNotFoundError(Selector.Text("Missing"))
          assertTrue(e1.message!!.contains("Missing"))

          val e2 = BackendUnavailableError("Shizuku", "Enable Wireless Debugging")
          assertTrue(e2.message!!.contains("Shizuku"))
          assertTrue(e2.setupHint.contains("Wireless Debugging"))

          val e3 = TargetCrashedError("com.example.app")
          assertTrue(e3.message!!.contains("com.example.app"))
      }
  }
  ```

- [ ] **Step 2: Run test — verify it fails**

  Run: `./gradlew :core:automation:test`  
  Expected: FAIL — `Selector`, `ElementNotFoundError`, `BackendUnavailableError`, `TargetCrashedError` not defined.

- [ ] **Step 3: Write `Selector.kt`**

  Sealed class with subtypes exactly matching the test: `Text`, `TextContains`, `TextMatches`, `ResourceId`, `Description`, `ClassName`, `Package`, `Clickable`, `Enabled`, `Index`, `Instance`, `XPath`, `Child(parent, child)`, `Sibling(anchor, sibling)`, `Compound(parts)`.

  Add extension function `Selector.Compound.flatten(): List<Selector>` — recursively unwraps nested `Compound` entries into a flat list with no `Compound` items.

- [ ] **Step 4: Write `AutomationException.kt`**

  ```kotlin
  sealed class AutomationException(message: String) : Exception(message)

  class ElementNotFoundError(val selector: Selector) :
      AutomationException("Element not found: $selector")

  class BackendUnavailableError(val backend: String, val setupHint: String) :
      AutomationException("Backend '$backend' unavailable. $setupHint")

  class ElementStaleError(val selector: Selector) :
      AutomationException("Element became stale: $selector")

  class TargetCrashedError(val packageName: String) :
      AutomationException("Target app '$packageName' crashed or disappeared")

  class TimeoutError(val selector: Selector, val timeoutMs: Long) :
      AutomationException("Timed out after ${timeoutMs}ms waiting for: $selector")
  ```

- [ ] **Step 5: Write `ElementInfo.kt`**

  ```kotlin
  data class ElementInfo(
      val text: String?,
      val description: String?,
      val resourceId: String?,
      val className: String?,
      val packageName: String?,
      val isClickable: Boolean,
      val isEnabled: Boolean,
      val isPassword: Boolean,       // if true, getText() returns "[REDACTED]"
      val boundsLeft: Int,
      val boundsTop: Int,
      val boundsRight: Int,
      val boundsBottom: Int,
  )
  ```

- [ ] **Step 6: Write `ShellResult.kt`**

  ```kotlin
  data class ShellResult(val stdout: String, val stderr: String, val exitCode: Int) {
      val isSuccess: Boolean get() = exitCode == 0
  }
  ```

- [ ] **Step 7: Write `UiElement.kt`**

  ```kotlin
  interface UiElement {
      suspend fun click()
      suspend fun longClick()
      suspend fun setText(text: String)
      suspend fun clearText()
      suspend fun getText(): String?        // returns "[REDACTED]" for password fields
      suspend fun exists(): Boolean
      suspend fun wait(timeoutMs: Long = 5_000): UiElement
      suspend fun waitGone(timeoutMs: Long = 5_000): Boolean
      fun bounds(): android.graphics.Rect
      fun info(): ElementInfo
      suspend fun scrollTo(selector: Selector): UiElement
      fun child(selector: Selector): UiElement
      fun sibling(selector: Selector): UiElement
  }
  ```

- [ ] **Step 8: Write `DeviceActions.kt`**

  ```kotlin
  enum class DeviceKey { BACK, HOME, RECENTS }

  interface DeviceActions {
      // Gestures
      suspend fun tap(x: Float, y: Float)
      suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300)
      suspend fun drag(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 500)
      suspend fun pinch(centerX: Float, centerY: Float, scale: Float)

      // Navigation
      suspend fun press(key: DeviceKey)
      suspend fun openNotifications()

      // Screen info
      suspend fun screenshot(filename: String? = null): ByteArray
      suspend fun windowSize(): Pair<Int, Int>   // width, height in pixels
      suspend fun currentApp(): String?           // returns foreground package name

      // App control
      suspend fun appStart(packageName: String, activity: String? = null)
      suspend fun appStop(packageName: String)
      suspend fun appClear(packageName: String)        // requires Shizuku
      suspend fun appInstall(apkPath: String)

      // Shell (requires Shizuku)
      suspend fun shell(command: String): ShellResult

      // Element lookup
      fun find(selector: Selector): UiElement
      fun findAll(selector: Selector): List<UiElement>
  }
  ```

- [ ] **Step 9: Run test — verify it passes**

  Run: `./gradlew :core:automation:test`  
  Expected: PASS (3 tests, 0 failures).

- [ ] **Step 10: Commit**

  ```
  git add core/automation/
  git commit -m "feat(phase-0): core:automation interfaces and exception hierarchy"
  ```

---

## Task 4: `:app` — Hilt + Compose Shell

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/droidpilot/app/DroidPilotApp.kt`
- Create: `app/src/main/kotlin/com/droidpilot/app/MainActivity.kt`
- Create: `app/src/main/kotlin/com/droidpilot/app/di/AppModule.kt`
- Create: `app/src/main/kotlin/com/droidpilot/app/navigation/AppNavHost.kt`
- Create: `app/src/main/kotlin/com/droidpilot/app/ui/HomeScreen.kt`
- Create: `app/src/main/kotlin/com/droidpilot/app/ui/theme/Theme.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/test/kotlin/com/droidpilot/app/DroidPilotAppTest.kt`

**Interfaces:**
- Consumes: nothing from other modules yet (shell only)
- Produces: `Route` sealed class (navigation destinations) — feature modules add entries later

- [ ] **Step 1: Write failing test in `DroidPilotAppTest.kt`**

  ```kotlin
  package com.droidpilot.app

  import dagger.hilt.android.HiltAndroidApp
  import org.junit.Assert.assertNotNull
  import org.junit.Test

  class DroidPilotAppTest {
      @Test
      fun `DroidPilotApp is annotated with HiltAndroidApp`() {
          val annotation = DroidPilotApp::class.java.getAnnotation(HiltAndroidApp::class.java)
          assertNotNull("@HiltAndroidApp annotation missing", annotation)
      }
  }
  ```

- [ ] **Step 2: Run test — verify it fails**

  Run: `./gradlew :app:test`  
  Expected: FAIL — `DroidPilotApp` class not found.

- [ ] **Step 3: Write `app/build.gradle.kts`**

  ```kotlin
  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)
      alias(libs.plugins.kotlin.kapt)
      alias(libs.plugins.hilt)
  }
  android {
      namespace = "com.droidpilot.app"
      compileSdk = libs.versions.compileSdk.get().toInt()
      defaultConfig {
          applicationId = "com.droidpilot.app"
          minSdk = 26
          targetSdk = libs.versions.targetSdk.get().toInt()
          versionCode = 1
          versionName = "0.1.0"
      }
      buildFeatures { compose = true }
      composeOptions { kotlinCompilerExtensionVersion = "<from BOM, verify>" }
      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_17
          targetCompatibility = JavaVersion.VERSION_17
      }
      kotlinOptions { jvmTarget = "17" }
  }
  dependencies {
      implementation(platform(libs.compose.bom))
      implementation(libs.compose.ui)
      implementation(libs.compose.material3)
      implementation(libs.compose.ui.tooling.preview)
      debugImplementation(libs.compose.ui.tooling)
      implementation(libs.hilt.android)
      kapt(libs.hilt.compiler)
      implementation(libs.navigation.compose)
      implementation(libs.coroutines.android)
      testImplementation(libs.junit)
      testImplementation(libs.hilt.android)        // for annotation check in test
  }
  ```

  Add `targetSdk` alias to `libs.versions.toml` (same value as `compileSdk`).

- [ ] **Step 4: Write `AndroidManifest.xml`**

  Declare `DroidPilotApp` as `android:name`, `MainActivity` as the launcher activity. No extra permissions at this phase.

- [ ] **Step 5: Write `DroidPilotApp.kt`**

  ```kotlin
  @HiltAndroidApp
  class DroidPilotApp : Application()
  ```

- [ ] **Step 6: Write `AppModule.kt`**

  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  object AppModule  // empty; populated in later phases
  ```

- [ ] **Step 7: Write `AppNavHost.kt`**

  ```kotlin
  sealed class Route(val path: String) {
      object Home : Route("home")
  }

  @Composable
  fun AppNavHost(navController: NavHostController = rememberNavController()) {
      NavHost(navController, startDestination = Route.Home.path) {
          composable(Route.Home.path) { HomeScreen() }
      }
  }
  ```

- [ ] **Step 8: Write `HomeScreen.kt`**

  A `@Composable` that displays a centered `Text("DroidPilot")` and a subtitle `Text("Phase 0 — Scaffold")`. Material 3 `Scaffold` wrapper.

- [ ] **Step 9: Write `Theme.kt`**

  Standard Material 3 `DroidPilotTheme` using `dynamicColorScheme` on API 31+ with a fallback purple baseline palette.

- [ ] **Step 10: Write `MainActivity.kt`**

  ```kotlin
  @AndroidEntryPoint
  class MainActivity : ComponentActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContent { DroidPilotTheme { AppNavHost() } }
      }
  }
  ```

- [ ] **Step 11: Run unit test — verify it passes**

  Run: `./gradlew :app:test`  
  Expected: PASS.

- [ ] **Step 12: Verify Compose compiler annotation processor**

  Run: `./gradlew :app:compileDebugKotlin`  
  Expected: SUCCESS with no annotation processor errors. If a Compose/Kotlin version mismatch error appears, fix `libs.versions.toml` before proceeding.

- [ ] **Step 13: Commit**

  ```
  git add app/
  git commit -m "feat(phase-0): app shell with Hilt and Compose navigation"
  ```

---

## Task 5: `:sample-target` Demo App

**Files:**
- Create: `sample-target/build.gradle.kts`
- Create: `sample-target/src/main/AndroidManifest.xml`
- Create: `sample-target/src/main/kotlin/com/droidpilot/sampletarget/MainActivity.kt`
- Create: `sample-target/src/main/kotlin/com/droidpilot/sampletarget/LoginActivity.kt`
- Create: `sample-target/src/main/kotlin/com/droidpilot/sampletarget/ItemListActivity.kt`
- Create: `sample-target/src/main/kotlin/com/droidpilot/sampletarget/ItemDetailDialogActivity.kt`
- Create: `sample-target/src/main/kotlin/com/droidpilot/sampletarget/WebViewActivity.kt`
- Create: `sample-target/src/main/res/layout/activity_login.xml`
- Create: `sample-target/src/main/res/layout/activity_item_list.xml`
- Create: `sample-target/src/main/res/layout/item_row.xml`
- Create: `sample-target/src/main/assets/index.html`
- Create: `sample-target/src/main/res/values/strings.xml`
- Create: `sample-target/src/test/kotlin/com/droidpilot/sampletarget/LoginValidationTest.kt`

**Interfaces:**
- Produces: installable APK at `sample-target/build/outputs/apk/debug/sample-target-debug.apk`
- Pinned resource IDs (used by Phase 1 instrumented tests — **do not change**):

  | View | Resource ID | Content Description |
  |------|------------|---------------------|
  | Username EditText | `@+id/username` | — |
  | Password EditText | `@+id/password` | — |
  | Login Button | `@+id/btnLogin` | `"Login"` |
  | Error TextView | `@+id/tvLoginError` | — |
  | Item RecyclerView | `@+id/rvItems` | — |
  | WebView button | `@+id/btnOpenWebView` | `"Open WebView"` |
  | Dialog OK button | uses `AlertDialog.Builder` default `"OK"` text | — |

- [ ] **Step 1: Write failing test in `LoginValidationTest.kt`**

  ```kotlin
  package com.droidpilot.sampletarget

  import org.junit.Assert.assertFalse
  import org.junit.Assert.assertTrue
  import org.junit.Test

  class LoginValidationTest {
      @Test
      fun `valid credentials accepted`() {
          assertTrue(LoginActivity.isValidCredentials("qa_user", "secret"))
      }

      @Test
      fun `empty username rejected`() {
          assertFalse(LoginActivity.isValidCredentials("", "secret"))
      }

      @Test
      fun `empty password rejected`() {
          assertFalse(LoginActivity.isValidCredentials("qa_user", ""))
      }

      @Test
      fun `wrong credentials rejected`() {
          assertFalse(LoginActivity.isValidCredentials("admin", "wrong"))
      }
  }
  ```

- [ ] **Step 2: Run test — verify it fails**

  Run: `./gradlew :sample-target:test`  
  Expected: FAIL — `LoginActivity` not found.

- [ ] **Step 3: Write `sample-target/build.gradle.kts`**

  Uses `android-application` plugin with no Compose (Views only, keeps the target simple and closer to real-world apps):

  ```kotlin
  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)
  }
  android {
      namespace = "com.droidpilot.sampletarget"
      compileSdk = libs.versions.compileSdk.get().toInt()
      defaultConfig {
          applicationId = "com.droidpilot.sampletarget"
          minSdk = 26
          targetSdk = libs.versions.targetSdk.get().toInt()
          versionCode = 1
          versionName = "1.0"
      }
  }
  dependencies {
      implementation("androidx.recyclerview:recyclerview:1.3.2")   // add alias to catalog
      testImplementation(libs.junit)
  }
  ```

- [ ] **Step 4: Write `LoginActivity.kt`**

  - Views: `username` EditText (inputType `text`), `password` EditText (inputType `textPassword`), `btnLogin` Button, `tvLoginError` TextView (initially `GONE`)
  - On button click: call `isValidCredentials()`; if true start `ItemListActivity`; if false show `tvLoginError` with text `"Invalid credentials"`
  - Companion object with `fun isValidCredentials(username: String, password: String): Boolean` — returns `true` only for `("qa_user", "secret")`

- [ ] **Step 5: Write `activity_login.xml`**

  `LinearLayout` (vertical) with the four views in the order listed above, using the exact resource IDs from the pinned ID table. `password` EditText must have `android:inputType="textPassword"`.

- [ ] **Step 6: Write `ItemListActivity.kt` and `activity_item_list.xml`**

  - `rvItems` RecyclerView with 5 hardcoded string items: `"Item 1"` through `"Item 5"`
  - Tapping any item shows an `AlertDialog` with `message = "Detail: <item text>"` and a single `"OK"` button (uses `AlertDialog.Builder`)
  - `btnOpenWebView` Button at the bottom; clicking it starts `WebViewActivity`

- [ ] **Step 7: Write `WebViewActivity.kt`**

  - Contains a `WebView` that loads `file:///android_asset/index.html`
  - WebSettings: `javaScriptEnabled = true`

- [ ] **Step 8: Write `sample-target/src/main/assets/index.html`**

  ```html
  <!DOCTYPE html>
  <html>
  <head><title>DroidPilot Sample</title></head>
  <body>
    <h1 id="heading">Sample WebView</h1>
    <p id="status">Ready</p>
    <button id="btnAction" onclick="document.getElementById('status').innerText='Clicked'">
      Click Me
    </button>
  </body>
  </html>
  ```

- [ ] **Step 9: Write `AndroidManifest.xml`**

  Launcher activity: `LoginActivity`. Declare `ItemListActivity`, `WebViewActivity`. Add `android:usesCleartextTraffic="true"` for the WebView to load local assets.

- [ ] **Step 10: Run unit tests — verify they pass**

  Run: `./gradlew :sample-target:test`  
  Expected: PASS (4 tests).

- [ ] **Step 11: Build sample-target APK**

  Run: `./gradlew :sample-target:assembleDebug`  
  Expected: APK at `sample-target/build/outputs/apk/debug/sample-target-debug.apk`.

- [ ] **Step 12: Commit**

  ```
  git add sample-target/
  git commit -m "feat(phase-0): sample-target demo app (login, list, dialog, webview)"
  ```

---

## Task 6: Integration Verification & Phase Gate

**Files:** none new — verification only

- [ ] **Step 1: Full project build**

  Run: `./gradlew assembleDebug`  
  Expected: BUILD SUCCESSFUL — all modules, including `:app` and `:sample-target`, produce APKs or AARs.

- [ ] **Step 2: Full unit test suite**

  Run: `./gradlew test`  
  Expected: all tests pass. Current test count: ≥ 7 (3 in `SelectorTest`, 1 in `DroidPilotAppTest`, 4 in `LoginValidationTest`).

- [ ] **Step 3: Lint**

  Run: `./gradlew ktlintCheck detekt`  
  Expected: no errors (warnings acceptable). Fix any errors before proceeding.

- [ ] **Step 4: Update CLAUDE.md phase checklist**

  Mark all Phase 0 items complete in `CLAUDE.md`.

- [ ] **Step 5: Final commit**

  ```
  git add CLAUDE.md
  git commit -m "feat(phase-0): scaffold complete — all modules, Hilt, Compose shell, sample-target"
  ```

---

## Self-Review Notes

**Spec coverage:**
- Multi-module Gradle: Tasks 1–2 ✓
- Version catalog: Task 1 ✓
- Hilt in `:app`: Task 4 ✓
- Compose shell: Task 4 ✓
- `:sample-target` with login, list, dialog, WebView: Task 5 ✓
- `assembleDebug` + `test` gate: Task 6 ✓
- `CLAUDE.md` checklist updated: Task 6 ✓

**Review Focus coverage:**
1. Compose/Kotlin mismatch → Task 4 Step 12 ✓
2. `@HiltAndroidApp` missing → Task 4 Step 1–2 (unit test) ✓
3. Dependency cycles → Task 2 Step 3 ✓
4. Sample-target resource IDs pinned in interface table → Task 5 ✓
5. Gradle wrapper version → Task 1 Step 7 ✓
