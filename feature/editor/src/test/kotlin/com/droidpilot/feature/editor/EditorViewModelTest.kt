package com.droidpilot.feature.editor

import androidx.lifecycle.SavedStateHandle
import java.net.URLEncoder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private fun makeVm(dirPath: String = "/tmp/proj"): EditorViewModel {
        val handle = SavedStateHandle(
            mapOf(
                "projectId" to 1L,
                "encodedDirPath" to URLEncoder.encode(dirPath, "UTF-8"),
            )
        )
        return EditorViewModel(handle)
    }

    @Test fun openFile_addsTab() = runTest(dispatcher) {
        val vm = makeVm()
        vm.openFile("/tmp/proj/test_login.py")
        // allow coroutine to run
        val tabs = vm.tabs.first { it.isNotEmpty() }
        assertEquals(1, tabs.size)
        assertEquals("/tmp/proj/test_login.py", tabs[0].path)
    }

    @Test fun openSameFileTwice_doesNotDuplicateTab() = runTest(dispatcher) {
        val vm = makeVm()
        vm.openFile("/tmp/proj/test_login.py")
        vm.tabs.first { it.isNotEmpty() }
        vm.openFile("/tmp/proj/test_login.py")
        assertEquals(1, vm.tabs.value.size)
    }

    @Test fun closeTab_removesTab() = runTest(dispatcher) {
        val vm = makeVm()
        vm.openFile("/tmp/proj/a.py")
        vm.tabs.first { it.isNotEmpty() }
        vm.openFile("/tmp/proj/b.py")
        vm.tabs.first { it.size == 2 }
        vm.closeTab(0)
        assertEquals(1, vm.tabs.value.size)
        assertEquals("/tmp/proj/b.py", vm.tabs.value[0].path)
    }

    @Test fun onContentChanged_marksTabDirty() = runTest(dispatcher) {
        val vm = makeVm()
        vm.openFile("/tmp/proj/t.py")
        vm.tabs.first { it.isNotEmpty() }
        vm.onContentChanged("print('hello')")
        assertTrue(vm.tabs.value[0].isDirty)
    }
}
