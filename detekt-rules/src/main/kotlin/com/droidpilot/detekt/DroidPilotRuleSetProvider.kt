package com.droidpilot.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class DroidPilotRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("droidpilot")
    override fun instance(): RuleSet = RuleSet(ruleSetId, listOf(::AccessibilityNodeInfoLeakRule))
}
