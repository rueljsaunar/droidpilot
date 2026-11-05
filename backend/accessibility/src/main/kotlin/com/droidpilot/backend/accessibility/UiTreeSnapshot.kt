package com.droidpilot.backend.accessibility

internal data class UiTreeSnapshot(
    val root: SerializedNode?,
    val timestamp: Long,
) {
    companion object {
        fun serialize(node: NodeInfo): SerializedNode {
            val children = (0 until node.childCount).map { i ->
                serialize(node.childAt(i))
            }
            return SerializedNode(
                text = if (node.isPassword) null else node.text,
                contentDescription = node.contentDescription,
                resourceId = node.resourceId,
                className = node.className,
                packageName = node.packageName,
                isClickable = node.isClickable,
                isEnabled = node.isEnabled,
                isPassword = node.isPassword,
                isChecked = node.isChecked,
                bounds = node.bounds,
                indexInParent = node.indexInParent,
                children = children,
            )
        }
    }
}
