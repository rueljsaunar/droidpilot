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
