package com.droidpilot.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpilot.app.ui.theme.LocalDpColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateUp: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About DroidPilot",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { AppHeader() }
            item { CreatorSection() }
            item { TutorialSection() }
            item { DocumentationSection() }
        }
    }
}

@Composable
private fun AppHeader() {
    val dpColors = LocalDpColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "{ }",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "DroidPilot",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                "v0.1.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
            )
            HorizontalDivider(
                modifier = Modifier
                    .width(64.dp)
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                "Android UI Automation IDE",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                "Write, run, and debug Python-based end-to-end tests on real Android devices.",
                style = MaterialTheme.typography.bodySmall,
                color = dpColors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CreatorSection() {
    SectionCard(icon = Icons.Default.Person, title = "Creator", initiallyExpanded = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CreatorRow("Organization", "77global")
            CreatorRow("Version", "0.1.0")
            CreatorRow("Platform", "Android 8.0+ (API 26+)")
            CreatorRow("Language", "Kotlin 2.x · Python 3.10")
            CreatorRow("UI Framework", "Jetpack Compose + Material 3")

            Spacer(Modifier.height(4.dp))

            Text(
                "BUILT WITH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("Chaquopy", "Hilt", "Room", "Sora Editor", "Shizuku").forEach {
                    TechBadge(it)
                }
            }
        }
    }
}

@Composable
private fun CreatorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TechBadge(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun TutorialSection() {
    val entries = listOf(
        TutorialEntry(
            "1 — Getting Started",
            "Enable Accessibility Service and grant required permissions.",
            """Step 1: Enable Accessibility Service
• Settings → Accessibility → Installed Services
• Find "DroidPilot" and toggle it ON
• Grant "Full Control" when prompted

Step 2: Grant Overlay Permission
• If the banner appears, tap "Grant"
• Find DroidPilot → enable "Display over other apps"

Step 3 (Optional): Set Up Shizuku
• Install the Shizuku app from Play Store
• Enable Wireless Debugging in Developer Options
• Pair Shizuku using the ADB pairing code
• Required for: app install, clear data, shell commands""",
        ),
        TutorialEntry(
            "2 — Creating a Project",
            "Organize your automation scripts in projects.",
            """1. Tap the + button on the Projects screen
2. Enter a project name and tap "Create"
3. Tap the project card to open the editor
4. Create a Python test file: e.g. test_login.py
5. Configure the target app:
   • Tap the ⋮ menu → Settings on the project card
   • Enter the target app package name
     e.g. com.example.myapp
   • Set timeout and retry defaults""",
        ),
        TutorialEntry(
            "3 — Writing Your First Test",
            "Use the droidpilot Python API to automate UI interactions.",
            """from droidpilot import device as d, expect

def setup():
    d.app_start("com.example.myapp")

def test_login():
    d.element(id="com.example.myapp:id/username").set_text("testuser")
    d.element(id="com.example.myapp:id/password").set_text("secret")
    d.element(text="Sign In").click()
    expect(d.element(text="Welcome")).to_be_visible(timeout=5)

def teardown():
    d.app_stop("com.example.myapp")""",
        ),
        TutorialEntry(
            "4 — Using the Inspector",
            "Capture live UI trees and copy stable selectors.",
            """1. Navigate to the target app screen you want to automate
2. Tap the Inspector icon in the DroidPilot toolbar
3. Tap "Capture" to snapshot the current screen
4. Browse the UI hierarchy tree
5. Tap an element to view all its properties:
   • Resource ID, text, description, class name
   • Bounds, clickable, focusable, scrollable flags
6. Tap "Copy Selector" to copy the best selector
7. Tap "Save to Repository" to store it by a friendly name
8. Tap "Insert into editor" to place it at the cursor""",
        ),
        TutorialEntry(
            "5 — Recording Actions",
            "Capture tap and text sequences automatically.",
            """1. Tap the Record icon (•) in the toolbar
   OR tap the overlay bubble → Record
2. Navigate through the target app — taps and
   text changes are captured automatically
3. Return to DroidPilot:
   • Tap the overlay → Review steps
   OR navigate back to the Recorder screen
4. Edit any step inline — fix selectors or text
5. Delete unwanted steps with the x button
6. Tap "Insert into editor" to place all steps
   at the active cursor position in the editor""",
        ),
        TutorialEntry(
            "6 — Running Tests & Viewing Reports",
            "Execute tests and analyze results.",
            """Running Tests:
• Open a test file → tap the Run button
• The Run Console shows live log output below
• The overlay bubble shows pass/fail counters
• Tap the bubble to expand and see the current step

Stopping a Run:
• Tap the Stop button in the overlay bubble
• OR tap the persistent notification → Stop action

Viewing Reports:
• From Projects → tap the project card menu → Run History
• Select a run to see test-level results
• Tap "Share Report" to export the HTML report
• Reports include timing, steps, and full log output""",
        ),
    )
    SectionCard(icon = Icons.Default.School, title = "Tutorial", initiallyExpanded = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { TutorialEntryCard(it) }
        }
    }
}

@Composable
private fun TutorialEntryCard(entry: TutorialEntry) {
    val dpColors = LocalDpColors.current
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dpColors.codeSurface)
                    .padding(12.dp),
            ) {
                Text(
                    entry.content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DocumentationSection() {
    val categories = listOf(
        ApiCategory(
            "Element Selection",
            "Locate UI elements by attribute or by saved repository name.",
            """# By resource ID (most stable — unique per app build)
d.element(id="com.pkg:id/btn_login")

# By visible text (exact match)
d.element(text="Sign In")

# By content description (accessibility label)
d.element(desc="Close dialog")

# By Java class name
d.element(cls="android.widget.EditText")

# Multiple attributes (all must match)
d.element(text="OK", cls="android.widget.Button")

# From Object Repository (by saved friendly name)
d.repo("Login button")""",
        ),
        ApiCategory(
            "Element Actions",
            "Interact with a located UiElement.",
            """element = d.element(id="com.pkg:id/input_username")

element.click()                   # tap the element
element.long_click()              # long-press (~1 second)
element.set_text("hello world")   # clear and type text
text   = element.get_text()       # returns str (or "")
exists = element.exists()         # returns bool
rect   = element.bounds()         # Rect(left,top,right,bottom)""",
        ),
        ApiCategory(
            "Assertions & Waits",
            "Assert UI state; block until an element appears or disappears.",
            """from droidpilot import expect

# Assert element is visible within timeout seconds
expect(d.element(text="Welcome")).to_be_visible(timeout=5)

# Assert element is gone within timeout seconds
expect(d.element(id="pkg:id/loading")).to_be_gone(timeout=3)

# Wait and return the element when found
el = d.wait(d.element(text="Done"), timeout=10)

# Wait until element disappears
d.wait_gone(d.element(id="pkg:id/spinner"), timeout=10)

# Raises AssertionError on timeout""",
        ),
        ApiCategory(
            "Gestures & Input",
            "Low-level gesture dispatch: tap, swipe, drag, pinch.",
            """# Tap at absolute screen coordinates
d.tap(540, 960)

# Swipe from (x1,y1) to (x2,y2) over duration_ms
d.swipe(100, 1200, 100, 400, duration_ms=300)

# Drag (useful for drag-and-drop targets)
d.drag(src_x=200, src_y=400, dst_x=600, dst_y=400, duration_ms=500)

# Pinch to zoom (scale > 1 zooms in, < 1 zooms out)
d.pinch(center_x=540, center_y=960, scale=2.0)

# Screen dimensions in pixels
width, height = d.window_size()""",
        ),
        ApiCategory(
            "App Control",
            "Launch, stop, and manage the app-under-test. Shell commands require Shizuku.",
            """# Launch app by package name
d.app_start("com.example.myapp")

# Stop the app process
d.app_stop("com.example.myapp")

# Clear all app data and cache
d.app_clear("com.example.myapp")

# Install APK from device storage (requires Shizuku)
d.app_install("/sdcard/Download/app-debug.apk")

# Run an arbitrary adb shell command (requires Shizuku)
output = d.shell("pm list packages | grep example")
print(output)  # → com.example.myapp""",
        ),
    )
    SectionCard(icon = Icons.Default.MenuBook, title = "API Reference", initiallyExpanded = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { ApiCategoryCard(it) }
        }
    }
}

@Composable
private fun ApiCategoryCard(category: ApiCategory) {
    val dpColors = LocalDpColors.current
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                )
                Text(category.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dpColors.codeSurface)
                    .padding(12.dp),
            ) {
                Text(
                    category.code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    initiallyExpanded: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                content = content,
            )
        }
    }
}

private data class TutorialEntry(val title: String, val subtitle: String, val content: String)
private data class ApiCategory(val name: String, val summary: String, val code: String)
