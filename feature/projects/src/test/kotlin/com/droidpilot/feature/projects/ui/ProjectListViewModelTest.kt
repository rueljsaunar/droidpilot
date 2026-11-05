package com.droidpilot.feature.projects.ui

import com.droidpilot.feature.projects.data.FakeProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val fakeRepo = FakeProjectRepository()

    private fun makeVm() = ProjectListViewModel(fakeRepo, dispatcher)

    @Test fun createProject_addsToList() = runTest(dispatcher) {
        makeVm().createProject("MyTests", "/tmp/MyTests")
        val projects = fakeRepo.observeAll().first()
        assertEquals(1, projects.size)
        assertEquals("MyTests", projects[0].name)
    }

    @Test fun renameProject_updatesName() = runTest(dispatcher) {
        val vm = makeVm()
        vm.createProject("Old", "/tmp/Old")
        val id = fakeRepo.observeAll().first()[0].id
        vm.renameProject(id, "New")
        val updated = fakeRepo.observeAll().first()[0]
        assertEquals("New", updated.name)
    }

    @Test fun deleteProject_removesFromList() = runTest(dispatcher) {
        val vm = makeVm()
        vm.createProject("ToDelete", "/tmp/ToDelete")
        val id = fakeRepo.observeAll().first()[0].id
        vm.deleteProject(id)
        assertTrue(fakeRepo.observeAll().first().isEmpty())
    }
}
