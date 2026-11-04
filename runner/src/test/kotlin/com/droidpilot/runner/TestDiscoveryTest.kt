package com.droidpilot.runner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TestDiscoveryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test fun `discoverInFile finds test functions and detects setup and teardown`() {
        val f = tmp.newFile("test_login.py")
        f.writeText(
            """
            def setup():
                pass
            def test_login_success():
                pass
            def test_login_failure():
                pass
            def teardown():
                pass
            """.trimIndent()
        )
        val cases = TestDiscovery.discoverInFile(f)
        assertEquals(2, cases.size)
        assertEquals("test_login_success", cases[0].functionName)
        assertEquals("test_login_failure", cases[1].functionName)
        assertTrue(cases[0].hasSetup)
        assertTrue(cases[0].hasTeardown)
    }

    @Test fun `discover returns only test_ prefixed py files`() {
        val dir = tmp.newFolder("project")
        dir.resolve("helpers.py").writeText("def helper(): pass")
        dir.resolve("test_suite.py").writeText("def test_one(): pass")
        val cases = TestDiscovery.discover(dir.absolutePath)
        assertEquals(1, cases.size)
        assertEquals("test_one", cases[0].functionName)
    }

    @Test fun `discoverInFile returns empty for file with no test functions`() {
        val f = tmp.newFile("test_empty.py")
        f.writeText("def helper(): pass")
        assertEquals(emptyList<TestCase>(), TestDiscovery.discoverInFile(f))
    }

    @Test fun `discoverInFile detects absence of setup and teardown`() {
        val f = tmp.newFile("test_a.py")
        f.writeText("def test_one(): pass")
        val cases = TestDiscovery.discoverInFile(f)
        assertEquals(1, cases.size)
        assertFalse(cases[0].hasSetup)
        assertFalse(cases[0].hasTeardown)
    }

    @Test fun `discover returns empty for non-existent directory`() {
        assertEquals(emptyList<TestCase>(), TestDiscovery.discover("/no/such/dir"))
    }
}
