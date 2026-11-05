package com.droidpilot.feature.projects.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDaoTest {

    private lateinit var db: ProjectDatabase
    private lateinit var dao: ProjectDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ProjectDatabase::class.java,
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
        val id = dao.insert(
            ProjectEntity(
                name = "P",
                dirPath = "/p",
                createdAt = 1L,
                settings = ProjectSettingsEmbedded(targetPackage = "com.example", timeoutMs = 60_000),
            )
        )
        val loaded = dao.getById(id)!!
        assertEquals("com.example", loaded.settings.targetPackage)
        assertEquals(60_000L, loaded.settings.timeoutMs)
    }
}
