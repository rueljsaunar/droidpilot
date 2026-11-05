package com.droidpilot.core.automation

/** A node in a serialized accessibility UI tree, safe to hold across threads. */
public data class UiNode(
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val className: String?,
    val packageName: String?,
    val isClickable: Boolean,
    val isEnabled: Boolean,
    val isPassword: Boolean,
    val isChecked: Boolean,
    val bounds: Bounds,
    val indexInParent: Int,
    val children: List<UiNode>,
) {
    /** Best single-attribute display label for tree views. */
    val label: String
        get() = text?.takeIf { it.isNotBlank() }
            ?: contentDescription?.takeIf { it.isNotBlank() }
            ?: resourceId?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: className?.substringAfterLast('.')
            ?: "<node>"

    /** Stable candidate selectors ranked most→least stable. */
    val selectorCandidates: List<Selector>
        get() = buildList {
            resourceId?.takeIf { it.isNotBlank() }?.let { add(Selector.ResourceId(it)) }
            contentDescription?.takeIf { it.isNotBlank() }?.let { add(Selector.Description(it)) }
            text?.takeIf { it.isNotBlank() }?.let { add(Selector.Text(it)) }
            className?.takeIf { it.isNotBlank() }?.let { add(Selector.ClassName(it)) }
        }
}
