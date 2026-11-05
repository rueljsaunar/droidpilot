package com.droidpilot.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * Flags any non-local property whose declared type is AccessibilityNodeInfo
 * unless it is inside NodeHolder itself (which is the safe owner of such nodes).
 */
class AccessibilityNodeInfoLeakRule(config: Config = Config.empty) : Rule(
    config,
    "AccessibilityNodeInfo must be held only in NodeHolder — holding it as a field causes a leak",
) {
    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.isLocal) return
        val typeText = property.typeReference?.text ?: return
        if (!typeText.contains("AccessibilityNodeInfo")) return
        val containingClass = property.getParentOfType<KtClass>(strict = true)
        if (containingClass?.name == "NodeHolder") return
        report(Finding(Entity.atName(property), "AccessibilityNodeInfo must be wrapped in NodeHolder"))
    }
}
