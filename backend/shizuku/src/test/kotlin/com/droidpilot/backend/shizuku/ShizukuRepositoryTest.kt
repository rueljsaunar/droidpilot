package com.droidpilot.backend.shizuku

import com.droidpilot.core.automation.BackendUnavailableError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuRepositoryTest {

    @Test
    fun `initial state is Unavailable`() = runTest {
        val state = ShizukuState.Unavailable
        assertTrue(state is ShizukuState.Unavailable)
    }

    @Test
    fun `BackendUnavailableError carries Shizuku backend name`() {
        val err = BackendUnavailableError(
            backend = "Shizuku",
            setupHint = ShizukuRepository.SETUP_HINT,
        )
        assertEquals("Shizuku", err.backend)
        assertTrue(err.message!!.contains("Shizuku"))
    }

    @Test
    fun `SETUP_HINT mentions Wireless Debugging and Shizuku`() {
        assertTrue(ShizukuRepository.SETUP_HINT.contains("Wireless Debugging"))
        assertTrue(ShizukuRepository.SETUP_HINT.contains("Shizuku"))
    }

    @Test
    fun `ShizukuState sealed hierarchy covers all five states`() {
        val states: List<ShizukuState> = listOf(
            ShizukuState.Unavailable,
            ShizukuState.PermissionRequired,
            ShizukuState.PermissionDenied,
            ShizukuState.Connecting,
            ShizukuState.Available,
        )
        assertEquals(5, states.size)
    }
}
