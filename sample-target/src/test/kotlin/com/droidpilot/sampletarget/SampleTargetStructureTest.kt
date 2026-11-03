package com.droidpilot.sampletarget

import org.junit.Assert.assertNotNull
import org.junit.Test

class SampleTargetStructureTest {

    @Test
    fun `LoginActivity exists`() {
        val clazz = Class.forName("com.droidpilot.sampletarget.LoginActivity")
        assertNotNull(clazz)
    }

    @Test
    fun `ItemListActivity exists`() {
        val clazz = Class.forName("com.droidpilot.sampletarget.ItemListActivity")
        assertNotNull(clazz)
    }

    @Test
    fun `WebViewActivity exists`() {
        val clazz = Class.forName("com.droidpilot.sampletarget.WebViewActivity")
        assertNotNull(clazz)
    }

    @Test
    fun `ItemAdapter exists`() {
        val clazz = Class.forName("com.droidpilot.sampletarget.ItemAdapter")
        assertNotNull(clazz)
    }
}
