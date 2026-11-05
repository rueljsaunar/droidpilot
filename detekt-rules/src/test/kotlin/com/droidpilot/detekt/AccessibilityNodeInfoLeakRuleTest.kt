package com.droidpilot.detekt

import dev.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityNodeInfoLeakRuleTest {

    private val rule = AccessibilityNodeInfoLeakRule()

    @Test
    fun `flags property holding AccessibilityNodeInfo outside NodeHolder`() {
        val code = """
            import android.view.accessibility.AccessibilityNodeInfo
            class Foo {
                private val leaky: AccessibilityNodeInfo? = null
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not flag local variable holding AccessibilityNodeInfo`() {
        val code = """
            import android.view.accessibility.AccessibilityNodeInfo
            fun foo() {
                val local: AccessibilityNodeInfo? = null
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not flag property inside NodeHolder`() {
        val code = """
            import android.view.accessibility.AccessibilityNodeInfo
            internal class NodeHolder(private val node: AccessibilityNodeInfo)
        """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }
}
