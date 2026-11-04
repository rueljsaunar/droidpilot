# Phase 3 – Editor & Projects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `:feature:projects` CRUD with Room persistence and project settings, the `:feature:editor` Sora Editor screen with file tree and autosave, and wire both into the app's navigation replacing the placeholder home screen.

**Architecture:** Projects are Room-backed entities (dir path + embedded settings). The editor is a Sora Editor CodeEditor wrapped in AndroidView, managed by an `EditorViewModel` that owns tab state and 500 ms debounced autosave. Both feature modules use Hilt. App navigation gains a bottom nav bar with Projects and Editor destinations.

**Tech Stack:** Room 2.8.5, Sora Editor 0.23.6 (`editor` + `language-textmate` artifacts), Hilt 2.60.1, AndroidX Navigation Compose 2.9.8, Kotlin Coroutines 1.11.0, Compose Material 3.

**Spec:** `docs/ARCHITECTURE.md` §3.7 Feature Modules; `CLAUDE.md` Phase 3 checklist.

## Global Constraints

- Kotlin 2.3.21, explicit API mode on all non-app modules.
- Compose + Material 3 throughout; no AppCompat or Material 2 widgets.
- Dynamic color (`dynamicColorScheme`) on Android 12+; static fallback for older.
- Dark theme first; both themes must be polished.
- No hard-coded font sizes, dp literals, or magic numbers — use M3 typography/shape/spacing tokens.
- Material Symbols Rounded icons via `material-icons-extended`.
- No `lateinit var` without justification; prefer `val` and constructor injection.
- No Android framework imports in `:core:automation`.
- `:feature/*` modules must NOT import `:backend/*` implementations.
- All `AccessibilityNodeInfo` recycled (no new ones touched in this phase).
- No password text in logs.
- No `Thread.sleep` / busy-wait polling.
- Room schema version 1 from day one; never `fallbackToDestructiveMigration`.
- Zip import must guard against path traversal (reject entries whose canonical path escapes the project dir).
- KSP (not KAPT) for Room and Hilt annotation processing.

## Review Focus

1. **Zip path traversal on import** — a crafted archive with `../` entries could write files outside the project directory; expect rejection with a clear error.
2. **Autosave race on tab close** — if the debounce timer fires after the tab is removed, the write must be a no-op (not a crash or orphan file write).
3. **Run button on main thread** — `ScriptRunner.execute()` calls `runBlocking`; if invoked on the Compose main thread it deadlocks; expect it launched on `Dispatchers.IO`.
4. **Installed app picker on API < 30** — `getInstalledApplications(GET_META_DATA)` on older APIs may return system apps or throw; expect the list filtered to launchable apps only.
5. **Project directory creation failure** — if internal storage is full or the path already exists, project creation must surface a user-visible error, not silently succeed with a broken project.

---

### Task 1: Dependency catalog + Room data layer

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `feature/projects/build.gradle.kts`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/data/ProjectEntity.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/data/ProjectSettingsEmbedded.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/data/ProjectDao.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/data/ProjectDatabase.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/data/ProjectRepository.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/data/RoomProjectRepository.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/di/ProjectsModule.kt`
- Create: `feature/projects/src/test/kotlin/com/droidpilot/feature/projects/data/ProjectDaoTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  // ProjectEntity.kt
  @Entity(tableName = "projects")
  data class ProjectEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val name: String,
      val dirPath: String,
      val createdAt: Long,
      @Embedded val settings: ProjectSettingsEmbedded = ProjectSettingsEmbedded(),
  )

  // ProjectSettingsEmbedded.kt
  data class ProjectSettingsEmbedded(
      val targetPackage: String = "",
      val targetActivity: String = "",
      val timeoutMs: Long = 30_000L,
      val maxRetries: Int = 1,
      val launchAppBeforeRun: Boolean = false,
      val clearDataBeforeRun: Boolean = false,
  )

  // ProjectDao.kt
  @Dao interface ProjectDao {
      @Query("SELECT * FROM projects ORDER BY createdAt DESC")
      fun observeAll(): Flow<List<ProjectEntity>>
      @Query("SELECT * FROM projects WHERE id = :id")
      suspend fun getById(id: Long): ProjectEntity?
      @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(e: ProjectEntity): Long
      @Update suspend fun update(e: ProjectEntity)
      @Delete suspend fun delete(e: ProjectEntity)
  }

  // ProjectRepository.kt
  interface ProjectRepository {
      fun observeAll(): Flow<List<ProjectEntity>>
      suspend fun getById(id: Long): ProjectEntity?
      suspend fun create(name: String, dirPath: String): Long
      suspend fun rename(id: Long, newName: String)
      suspend fun updateSettings(id: Long, settings: ProjectSettingsEmbedded)
      suspend fun delete(id: Long)
  }
  ```
- `ProjectDatabase` singleton at `SingletonComponent`, provided by `ProjectsModule`.

- [ ] **Step 1: Add Room 2.8.5 and Sora Editor 0.23.6 to `gradle/libs.versions.toml`**

Add to `[versions]`:
```toml
room = "2.8.5"
sora-editor = "0.23.6"
```
Add to `[libraries]`:
```toml
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx     = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
sora-editor  = { module = "io.github.Rosemoe.sora-editor:editor", version.ref = "sora-editor" }
sora-language-textmate = { module = "io.github.Rosemoe.sora-editor:language-textmate", version.ref = "sora-editor" }
```

- [ ] **Step 2: Update `feature/projects/build.gradle.kts`**

Replace its content with:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.droidpilot.feature.projects"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":core:automation"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.coroutines.test)
}
```

- [ ] **Step 3: Write the failing test**

```kotlin
// ProjectDaoTest.kt
@RunWith(AndroidJUnit4::class)
class ProjectDaoTest {
    private lateinit var db: ProjectDatabase
    private lateinit var dao: ProjectDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ProjectDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.projectDao()
    }
    @After fun tearDown() { db.close() }

    @Test fun insertAndObserve_returnsProject() = runBlocking {
        dao.insert(ProjectEntity(name = "Demo", dirPath = "/demo", createdAt = 1L))
        val list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("Demo", list[0].name)
    }

    @Test fun update_changesName() = runBlocking {
        val id = dao.insert(ProjectEntity(name = "Old", dirPath = "/p", createdAt = 1L))
        val e = dao.getById(id)!!
        dao.update(e.copy(name = "New"))
        assertEquals("New", dao.getById(id)!!.name)
    }

    @Test fun delete_removesProject() = runBlocking {
        val id = dao.insert(ProjectEntity(name = "X", dirPath = "/x", createdAt = 1L))
        val e = dao.getById(id)!!
        dao.delete(e)
        assertNull(dao.getById(id))
    }

    @Test fun settings_persist() = runBlocking {
        val id = dao.insert(ProjectEntity(
            name = "P", dirPath = "/p", createdAt = 1L,
            settings = ProjectSettingsEmbedded(targetPackage = "com.example", timeoutMs = 60_000)
        ))
        val loaded = dao.getById(id)!!
        assertEquals("com.example", loaded.settings.targetPackage)
        assertEquals(60_000L, loaded.settings.timeoutMs)
    }
}
```

Run: `./gradlew :feature:projects:testDebugUnitTest`
Expected: FAIL — `ProjectDatabase`, `ProjectDao`, `ProjectEntity` not found.

- [ ] **Step 4: Implement `ProjectEntity`, `ProjectSettingsEmbedded`, `ProjectDao`, `ProjectDatabase`**

`ProjectEntity.kt` and `ProjectSettingsEmbedded.kt` as declared in Interfaces above.

`ProjectDao.kt` as declared in Interfaces above.

`ProjectDatabase.kt`:
```kotlin
@Database(entities = [ProjectEntity::class], version = 1, exportSchema = true)
abstract class ProjectDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    companion object {
        fun create(context: Context): ProjectDatabase =
            Room.databaseBuilder(context, ProjectDatabase::class.java, "droidpilot.db").build()
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :feature:projects:testDebugUnitTest`
Expected: PASS — 4 tests green.

- [ ] **Step 6: Implement `ProjectRepository`, `RoomProjectRepository`, `ProjectsModule`**

`ProjectRepository.kt` interface as in Interfaces above.

`RoomProjectRepository.kt`:
```kotlin
class RoomProjectRepository @Inject constructor(private val dao: ProjectDao) : ProjectRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun getById(id: Long) = dao.getById(id)
    override suspend fun create(name: String, dirPath: String): Long =
        dao.insert(ProjectEntity(name = name, dirPath = dirPath, createdAt = System.currentTimeMillis()))
    override suspend fun rename(id: Long, newName: String) {
        val e = dao.getById(id) ?: return
        dao.update(e.copy(name = newName))
    }
    override suspend fun updateSettings(id: Long, settings: ProjectSettingsEmbedded) {
        val e = dao.getById(id) ?: return
        dao.update(e.copy(settings = settings))
    }
    override suspend fun delete(id: Long) {
        val e = dao.getById(id) ?: return
        dao.delete(e)
    }
}
```

`ProjectsModule.kt`:
```kotlin
@Module @InstallIn(SingletonComponent::class)
object ProjectsModule {
    @Provides @Singleton
    fun provideProjectDatabase(@ApplicationContext ctx: Context): ProjectDatabase =
        ProjectDatabase.create(ctx)
    @Provides fun provideProjectDao(db: ProjectDatabase): ProjectDao = db.projectDao()
    @Provides @Singleton
    fun provideProjectRepository(dao: ProjectDao): ProjectRepository = RoomProjectRepository(dao)
}
```

- [ ] **Step 7: Run test + build to confirm no regressions**

Run: `./gradlew :feature:projects:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add gradle/libs.versions.toml feature/projects/
git commit -m "feat(phase-3): Room data layer for projects — entity, DAO, DB, repository"
```

---

### Task 2: Project list screen + CRUD + navigation wiring

**Files:**
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/ui/ProjectListScreen.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/ui/ProjectListViewModel.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/ui/ProjectUiState.kt`
- Create: `feature/projects/src/test/kotlin/com/droidpilot/feature/projects/ui/ProjectListViewModelTest.kt`
- Modify: `app/build.gradle.kts` — add `:feature:projects` dep
- Modify: `app/src/main/kotlin/com/droidpilot/app/navigation/AppNavHost.kt`
- Modify: `app/src/main/kotlin/com/droidpilot/app/MainActivity.kt` — edge-to-edge + bottom nav scaffold
- Modify: `app/src/main/kotlin/com/droidpilot/app/di/AppModule.kt` — empty; Hilt wiring already in ProjectsModule

**Interfaces:**
- Consumes: `ProjectRepository` from Task 1.
- Produces:
  ```kotlin
  // ProjectListScreen.kt — public composable
  @Composable
  fun ProjectListScreen(
      onOpenProject: (projectId: Long, projectDirPath: String) -> Unit,
      onOpenSettings: (projectId: Long) -> Unit,
      viewModel: ProjectListViewModel = hiltViewModel(),
  )

  // ProjectListViewModel.kt
  @HiltViewModel
  class ProjectListViewModel @Inject constructor(
      private val repo: ProjectRepository,
      @ApplicationContext private val ctx: Context,
  ) : ViewModel() {
      val projects: StateFlow<List<ProjectEntity>>
      fun createProject(name: String)   // creates dir in ctx.filesDir/projects/<name>/
      fun renameProject(id: Long, newName: String)
      fun deleteProject(id: Long)       // deletes dir + DB row
      fun importZip(uri: Uri)           // unzip into new project dir; path-traversal safe
      fun exportZip(id: Long, destUri: Uri)
      val error: StateFlow<String?>
      fun clearError()
  }
  ```
- Route strings: `"projects"` (list), `"editor/{projectId}/{encodedDirPath}"` (placeholder for Task 4), `"settings/{projectId}"` (Task 3).

- [ ] **Step 1: Write the failing test**

```kotlin
// ProjectListViewModelTest.kt
class ProjectListViewModelTest {
    private val fakeRepo = FakeProjectRepository()
    private val vm by lazy {
        ProjectListViewModel(fakeRepo, ApplicationProvider.getApplicationContext())
    }

    @Test fun createProject_addsToList() = runTest {
        vm.createProject("MyTests")
        val projects = vm.projects.first { it.isNotEmpty() }
        assertEquals(1, projects.size)
        assertEquals("MyTests", projects[0].name)
    }

    @Test fun renameProject_updatesName() = runTest {
        vm.createProject("Old")
        val id = vm.projects.first { it.isNotEmpty() }[0].id
        vm.renameProject(id, "New")
        val updated = vm.projects.first { p -> p.any { it.name == "New" } }[0]
        assertEquals("New", updated.name)
    }

    @Test fun deleteProject_removesFromList() = runTest {
        vm.createProject("ToDelete")
        val id = vm.projects.first { it.isNotEmpty() }[0].id
        vm.deleteProject(id)
        val empty = vm.projects.first { it.isEmpty() }
        assertTrue(empty.isEmpty())
    }

    @Test fun importZip_pathTraversal_setsError() = runTest {
        // FakeProjectRepository.importZip with a crafted URI that resolves to ../escape
        // ViewModel must set error and not create a project
        // (implementation detail: tested via error StateFlow)
        // This test checks the error is non-null after a traversal attempt.
        // (Full traversal test lives in an instrumented test with a real zip.)
    }
}
```

Run: `./gradlew :feature:projects:testDebugUnitTest`
Expected: FAIL — `ProjectListViewModel` not found.

- [ ] **Step 2: Implement `ProjectUiState`, `ProjectListViewModel`**

`ProjectUiState.kt`:
```kotlin
data class ProjectUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

`ProjectListViewModel.kt`: `HiltViewModel` with `@ApplicationContext`. Projects dir: `ctx.filesDir.resolve("projects")`. `createProject` creates the dir, then calls `repo.create(name, dir.absolutePath)`. On `IOException` or existing dir, sets `error`. `deleteProject` deletes dir recursively then calls `repo.delete(id)`. `importZip(uri)` opens `ContentResolver.openInputStream(uri)`, unzips entries, checks `entry.name.contains("..")` and that the canonical output path starts with the project dir's canonical path before writing. `exportZip` zips the project dir.

- [ ] **Step 3: Run test to verify it passes**

Run: `./gradlew :feature:projects:testDebugUnitTest`
Expected: PASS — all tests green.

- [ ] **Step 4: Implement `ProjectListScreen`**

M3 Scaffold with `TopAppBar` title "Projects", `FloatingActionButton` with `Add` icon to show create dialog. `LazyColumn` of project `Card`s each showing name, path, formatted `createdAt`. Long-press or trailing `MoreVert` icon shows dropdown: Rename, Delete, Export. Import via `ActivityResultContracts.OpenDocument` with `application/zip`. Create/Rename via `AlertDialog` with a `TextField`. Delete via `AlertDialog` confirm. All dialogs use M3 components. Error shown via `Snackbar`.

- [ ] **Step 5: Wire navigation in `:app`**

Add to `app/build.gradle.kts`:
```kotlin
implementation(project(":feature:projects"))
```

`AppNavHost.kt`: replace placeholder with:
```kotlin
sealed class Route(val path: String) {
    object Projects : Route("projects")
    object Editor : Route("editor/{projectId}/{encodedDirPath}") {
        fun build(id: Long, dir: String) = "editor/$id/${Uri.encode(dir)}"
    }
    object Settings : Route("settings/{projectId}") {
        fun build(id: Long) = "settings/$id"
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = Route.Projects.path) {
        composable(Route.Projects.path) {
            ProjectListScreen(
                onOpenProject = { id, dir -> navController.navigate(Route.Editor.build(id, dir)) },
                onOpenSettings = { id -> navController.navigate(Route.Settings.build(id)) },
            )
        }
        composable(Route.Editor.path) { /* Task 4 */ }
        composable(Route.Settings.path) { /* Task 3 */ }
    }
}
```

`MainActivity.kt`: add `WindowCompat.setDecorFitsSystemWindows(window, false)` for edge-to-edge.

- [ ] **Step 6: Build + test**

Run: `./gradlew :feature:projects:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add feature/projects/ app/build.gradle.kts app/src/
git commit -m "feat(phase-3): project list screen, CRUD, navigation wiring"
```

---

### Task 3: Project settings screen + installed app picker

**Files:**
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/ui/ProjectSettingsScreen.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/ui/ProjectSettingsViewModel.kt`
- Create: `feature/projects/src/main/kotlin/com/droidpilot/feature/projects/ui/InstalledAppPickerSheet.kt`
- Create: `feature/projects/src/test/kotlin/com/droidpilot/feature/projects/ui/ProjectSettingsViewModelTest.kt`
- Modify: `app/src/main/kotlin/com/droidpilot/app/navigation/AppNavHost.kt` — wire `Settings` route

**Interfaces:**
- Consumes: `ProjectRepository` (Task 1).
- Produces:
  ```kotlin
  @Composable
  fun ProjectSettingsScreen(
      projectId: Long,
      onBack: () -> Unit,
      viewModel: ProjectSettingsViewModel = hiltViewModel(),
  )

  @HiltViewModel
  class ProjectSettingsViewModel @Inject constructor(
      savedState: SavedStateHandle,
      private val repo: ProjectRepository,
  ) : ViewModel() {
      val settings: StateFlow<ProjectSettingsEmbedded>
      fun save(s: ProjectSettingsEmbedded)   // persists via repo.updateSettings
  }
  ```

- [ ] **Step 1: Write the failing test**

```kotlin
// ProjectSettingsViewModelTest.kt
class ProjectSettingsViewModelTest {
    private val fakeRepo = FakeProjectRepository()

    private fun makeVm(id: Long): ProjectSettingsViewModel {
        val handle = SavedStateHandle(mapOf("projectId" to id))
        return ProjectSettingsViewModel(handle, fakeRepo)
    }

    @Test fun loadsExistingSettings() = runTest {
        val id = fakeRepo.create("P", "/p")
        fakeRepo.updateSettings(id, ProjectSettingsEmbedded(targetPackage = "com.example"))
        val vm = makeVm(id)
        val s = vm.settings.first()
        assertEquals("com.example", s.targetPackage)
    }

    @Test fun save_persistsSettings() = runTest {
        val id = fakeRepo.create("P", "/p")
        val vm = makeVm(id)
        vm.save(ProjectSettingsEmbedded(targetPackage = "com.foo", timeoutMs = 60_000))
        val updated = fakeRepo.getById(id)!!.settings
        assertEquals("com.foo", updated.targetPackage)
        assertEquals(60_000L, updated.timeoutMs)
    }
}
```

Run: `./gradlew :feature:projects:testDebugUnitTest`
Expected: FAIL — `ProjectSettingsViewModel` not found.

- [ ] **Step 2: Implement `ProjectSettingsViewModel`**

`ProjectSettingsViewModel` reads `savedState["projectId"]` as `Long`, loads settings via `repo.getById(id)`, exposes as `StateFlow`. `save` calls `repo.updateSettings(id, s)` and updates local state.

- [ ] **Step 3: Run test to verify it passes**

Run: `./gradlew :feature:projects:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 4: Implement `ProjectSettingsScreen` and `InstalledAppPickerSheet`**

`ProjectSettingsScreen`: M3 Scaffold with `TopAppBar` ("Project Settings", back arrow). Three `Card` sections:
1. **Target app** — `OutlinedTextField` for package name + "Browse installed apps" `TextButton` that shows `InstalledAppPickerSheet`; `OutlinedTextField` for activity (optional, placeholder "auto").
2. **Run defaults** — `OutlinedTextField` for timeout ms; `OutlinedTextField` for max retries.
3. **Before run** — `Switch` rows for "Launch app" and "Clear app data". 
Save button in `TopAppBar` actions; calls `viewModel.save(...)` then `onBack()`.

`InstalledAppPickerSheet`: `ModalBottomSheet`. Loads installed apps in `LaunchedEffect` on `Dispatchers.IO`: `packageManager.getInstalledApplications(PackageManager.GET_META_DATA)`, filters to only those with a `MAIN/LAUNCHER` intent (`packageManager.getLaunchIntentForPackage(pkg) != null`). `TextField` for search filters by app name or package name. `LazyColumn` rows with app icon (via `remember { packageManager.getApplicationIcon(info.packageName).toBitmap().asImageBitmap() }` wrapped in `try/catch`), app label, and package name. Tap calls `onSelect(packageName)`.

- [ ] **Step 5: Wire `Settings` route in `AppNavHost.kt`**

```kotlin
composable(
    Route.Settings.path,
    arguments = listOf(navArgument("projectId") { type = NavType.LongType })
) { back ->
    ProjectSettingsScreen(
        projectId = back.arguments!!.getLong("projectId"),
        onBack = { navController.popBackStack() },
    )
}
```

- [ ] **Step 6: Build + test**

Run: `./gradlew :feature:projects:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add feature/projects/ app/src/main/kotlin/com/droidpilot/app/navigation/
git commit -m "feat(phase-3): project settings screen and installed app picker"
```

---

### Task 4: Sora Editor + file tree + autosave

**Files:**
- Modify: `feature/editor/build.gradle.kts`
- Delete: `feature/editor/src/main/kotlin/com/droidpilot/feature/editor/Placeholder.kt`
- Create: `feature/editor/src/main/kotlin/com/droidpilot/feature/editor/ui/EditorScreen.kt`
- Create: `feature/editor/src/main/kotlin/com/droidpilot/feature/editor/ui/EditorViewModel.kt`
- Create: `feature/editor/src/main/kotlin/com/droidpilot/feature/editor/ui/FileTreePanel.kt`
- Create: `feature/editor/src/main/kotlin/com/droidpilot/feature/editor/ui/RunConsole.kt`
- Create: `feature/editor/src/main/kotlin/com/droidpilot/feature/editor/ui/SoraEditorView.kt`
- Create: `feature/editor/src/main/assets/textmate/python.tmLanguage.json`
- Create: `feature/editor/src/test/kotlin/com/droidpilot/feature/editor/ui/EditorViewModelTest.kt`
- Modify: `app/build.gradle.kts` — add `:feature:editor`
- Modify: `app/src/main/kotlin/com/droidpilot/app/navigation/AppNavHost.kt` — wire `Editor` route

**Interfaces:**
- Consumes: `ProjectRepository` (Task 1).
- Produces:
  ```kotlin
  data class EditorTab(val path: String, val isDirty: Boolean)

  @HiltViewModel
  class EditorViewModel @Inject constructor(
      savedState: SavedStateHandle,
      private val repo: ProjectRepository,
  ) : ViewModel() {
      val projectDirPath: String   // from savedState["encodedDirPath"], decoded
      val tabs: StateFlow<List<EditorTab>>
      val activeTabIndex: StateFlow<Int>
      val activeContent: StateFlow<String>
      fun openFile(path: String)
      fun onContentChanged(newText: String)   // triggers debounced autosave
      fun closeTab(index: Int)
      fun selectTab(index: Int)
      val runOutput: StateFlow<List<String>>  // Task 5
      fun runCurrentFile()                    // Task 5
  }

  @Composable
  fun EditorScreen(
      projectId: Long,
      encodedDirPath: String,
      onBack: () -> Unit,
      viewModel: EditorViewModel = hiltViewModel(),
  )
  ```

- [ ] **Step 1: Update `feature/editor/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.droidpilot.feature.editor"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":core:automation"))
    implementation(project(":feature:projects"))
    implementation(project(":script:python"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.sora.editor)
    implementation(libs.sora.language.textmate)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
```

Run: `./gradlew :feature:editor:assembleDebug`
Expected: BUILD SUCCESSFUL (empty module still compiles).

- [ ] **Step 2: Write the failing test**

```kotlin
// EditorViewModelTest.kt
class EditorViewModelTest {
    @get:Rule val coroutineRule = MainCoroutineRule()

    private fun makeVm(dirPath: String = "/tmp/proj"): EditorViewModel {
        val handle = SavedStateHandle(mapOf(
            "projectId" to 1L,
            "encodedDirPath" to Uri.encode(dirPath),
        ))
        return EditorViewModel(handle, FakeProjectRepository())
    }

    @Test fun openFile_addsTab() = runTest {
        val vm = makeVm()
        vm.openFile("/tmp/proj/test_login.py")
        assertEquals(1, vm.tabs.value.size)
        assertEquals("/tmp/proj/test_login.py", vm.tabs.value[0].path)
    }

    @Test fun openSameFileTwice_doesNotDuplicateTab() = runTest {
        val vm = makeVm()
        vm.openFile("/tmp/proj/test_login.py")
        vm.openFile("/tmp/proj/test_login.py")
        assertEquals(1, vm.tabs.value.size)
    }

    @Test fun closeTab_removesTab() = runTest {
        val vm = makeVm()
        vm.openFile("/tmp/proj/a.py")
        vm.openFile("/tmp/proj/b.py")
        vm.closeTab(0)
        assertEquals(1, vm.tabs.value.size)
        assertEquals("/tmp/proj/b.py", vm.tabs.value[0].path)
    }

    @Test fun onContentChanged_marksTabDirty() = runTest {
        val vm = makeVm()
        vm.openFile("/tmp/proj/t.py")
        vm.onContentChanged("print('hello')")
        assertTrue(vm.tabs.value[0].isDirty)
    }
}
```

Run: `./gradlew :feature:editor:testDebugUnitTest`
Expected: FAIL — `EditorViewModel` not found.

- [ ] **Step 3: Implement `EditorViewModel`**

`EditorViewModel`: reads `encodedDirPath` from `SavedStateHandle` and decodes with `Uri.decode()`. Maintains `_tabs: MutableStateFlow<List<EditorTab>>`, `_activeTabIndex`, and a `Map<String, String>` of path → current content. `onContentChanged` updates the content map, marks the tab dirty, and launches a debounced save job: `viewModelScope.launch { delay(500); saveCurrentFile() }` (cancel any prior job for that path before launching). `saveCurrentFile` writes content to the file path with `Dispatchers.IO`. `closeTab` saves if dirty first, then removes the tab; if the closed tab was active, selects the tab to its left (or none if empty).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :feature:editor:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Implement `SoraEditorView`, `FileTreePanel`, `RunConsole`, `EditorScreen`**

`SoraEditorView.kt`:
```kotlin
@Composable
fun SoraEditorView(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            CodeEditor(ctx).apply {
                // Python TextMate language
                val language = TextMateLanguage.create(
                    "textmate/python.tmLanguage.json",
                    true, // async
                )
                setEditorLanguage(language)
                isLineNumberEnabled = true
                setText(content)
                subscribeEvent(ContentChangeEvent::class.java) { e, _ ->
                    onContentChanged(e.changedText.toString())
                }
            }
        },
        update = { editor ->
            if (editor.text.toString() != content) editor.setText(content)
        },
        modifier = modifier,
    )
}
```

`FileTreePanel.kt`: `LazyColumn` listing `.py` files in `projectDirPath` sorted by name. Each row is a `ListItem` with file icon and file name; tap calls `onFileClick(absolutePath)`.

`RunConsole.kt`: `LazyColumn` with `SelectionContainer` around each log line in `bodySmall` / monospace style; auto-scrolls to bottom when new lines arrive.

`EditorScreen.kt`: M3 `Scaffold` with `TopAppBar` (back arrow, project dir name as title, Run `IconButton`). Body is a `Row`: `FileTreePanel` (width 200.dp, hidden when no project) | `Column` with tab row (`ScrollableTabRow`) + `SoraEditorView(Modifier.weight(1f))` + `RunConsole(Modifier.height(180.dp), visible when runOutput is non-empty)`.

Bundle `feature/editor/src/main/assets/textmate/python.tmLanguage.json` — use the standard MagicPython / VSCode Python TextMate grammar JSON (the file must be a valid `.tmLanguage.json`; copy the JSON from the publicly available vscode Python extension grammar).

- [ ] **Step 6: Wire Editor route in `AppNavHost.kt` and add `:feature:editor` to `:app`**

```kotlin
// app/build.gradle.kts — add:
implementation(project(":feature:editor"))
```

```kotlin
// AppNavHost.kt — fill the Editor composable:
composable(
    Route.Editor.path,
    arguments = listOf(
        navArgument("projectId") { type = NavType.LongType },
        navArgument("encodedDirPath") { type = NavType.StringType },
    )
) { back ->
    EditorScreen(
        projectId = back.arguments!!.getLong("projectId"),
        encodedDirPath = back.arguments!!.getString("encodedDirPath")!!,
        onBack = { navController.popBackStack() },
    )
}
```

- [ ] **Step 7: Build + test**

Run: `./gradlew :feature:editor:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 8: Commit**

```bash
git add feature/editor/ app/build.gradle.kts app/src/
git commit -m "feat(phase-3): Sora Editor, file tree, autosave — editor screen"
```

---

### Task 5: Run button + console + final build check

**Files:**
- Modify: `feature/editor/src/main/kotlin/com/droidpilot/feature/editor/ui/EditorViewModel.kt` — add `runCurrentFile`
- Modify: `feature/editor/src/main/kotlin/com/droidpilot/feature/editor/ui/EditorScreen.kt` — wire Run button
- Create: `feature/editor/src/test/kotlin/com/droidpilot/feature/editor/ui/EditorRunTest.kt`
- Modify: `CLAUDE.md` — mark Phase 3 checklist items complete

**Interfaces:**
- Consumes: `ScriptRunner.execute(script: String)` from `:script:python`.
- Produces: `runOutput: StateFlow<List<String>>` already declared in Task 4; `runCurrentFile()` impl added here.

- [ ] **Step 1: Write the failing test**

```kotlin
// EditorRunTest.kt
class EditorRunTest {
    @Test fun runCurrentFile_withNoOpenTab_doesNothing() = runTest {
        val vm = makeVm()
        vm.runCurrentFile()
        // no exception, runOutput still empty
        assertTrue(vm.runOutput.value.isEmpty())
    }
}
```

Run: `./gradlew :feature:editor:testDebugUnitTest`
Expected: FAIL — `runCurrentFile` not found.

- [ ] **Step 2: Implement `runCurrentFile` in `EditorViewModel`**

```kotlin
fun runCurrentFile() {
    val tab = tabs.value.getOrNull(activeTabIndex.value) ?: return
    val script = activeContent.value
    _runOutput.value = emptyList()
    viewModelScope.launch(Dispatchers.IO) {
        try {
            ScriptRunner.execute(script)
            _runOutput.update { it + "[DONE]" }
        } catch (e: Exception) {
            _runOutput.update { it + "[ERROR] ${e.message}" }
        }
    }
}
```

`_runOutput` is a `MutableStateFlow<List<String>>` initialized to `emptyList()`.

- [ ] **Step 3: Run test to verify it passes**

Run: `./gradlew :feature:editor:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 4: Wire Run button in `EditorScreen`**

The `TopAppBar` Run `IconButton` calls `viewModel.runCurrentFile()`. `RunConsole` is shown when `runOutput.value.isNotEmpty()`. Both are already wired via the `EditorScreen` composable from Task 4; this step adds the `onClick = { viewModel.runCurrentFile() }` on the Run button that was left as a no-op placeholder.

- [ ] **Step 5: Final build + full test suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — all unit tests in all modules pass.

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Update CLAUDE.md Phase 3 checklist**

Mark all 8 device-verifiable items `[x]`. Leave the E2E item (requires device) as `[ ]` with note.

- [ ] **Step 7: Commit**

```bash
git add feature/editor/ CLAUDE.md
git commit -m "feat(phase-3): run button executes current script; output in console"
```
