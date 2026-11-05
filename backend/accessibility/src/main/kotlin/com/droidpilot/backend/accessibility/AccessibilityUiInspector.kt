package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.UiInspector
import com.droidpilot.core.automation.UiNode
import com.droidpilot.core.automation.Bounds
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AccessibilityUiInspector @Inject constructor() : UiInspector {

    override fun snapshot(): UiNode? =
        DroidPilotAccessibilityService.instance?.snapshot?.root?.toUiNode()

    override val treeChanges: Flow<Unit>
        get() = DroidPilotAccessibilityService.instance?.eventFlow
            ?: kotlinx.coroutines.flow.emptyFlow()
}

private fun SerializedNode.toUiNode(): UiNode = UiNode(
    text = text,
    contentDescription = contentDescription,
    resourceId = resourceId,
    className = className,
    packageName = packageName,
    isClickable = isClickable,
    isEnabled = isEnabled,
    isPassword = isPassword,
    isChecked = isChecked,
    bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
    indexInParent = indexInParent,
    children = children.map { it.toUiNode() },
)
