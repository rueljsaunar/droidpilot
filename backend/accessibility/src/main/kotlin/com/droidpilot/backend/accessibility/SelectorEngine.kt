package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.ElementNotFoundError
import com.droidpilot.core.automation.Selector

internal object SelectorEngine {

    fun find(selector: Selector, root: SerializedNode): SerializedNode =
        findAll(selector, root).firstOrNull()
            ?: throw ElementNotFoundError(selector)

    fun findAll(selector: Selector, root: SerializedNode): List<SerializedNode> =
        when (selector) {
            is Selector.XPath -> throw UnsupportedOperationException(
                "XPath selector is not supported in the accessibility backend"
            )
            is Selector.Instance -> {
                // Standalone Instance(n) returns the nth node in DFS order (use inside Compound for useful behavior)
                dfs(root).drop(selector.n).take(1)
            }
            is Selector.Index -> {
                // Index is position among parent's children; find nodes whose indexInParent == n
                dfs(root).filter { it.indexInParent == selector.index }
            }
            is Selector.Child -> {
                // Find all parents matching parent selector, then find child within each parent's subtree
                findAll(selector.parent, root).flatMap { parent ->
                    findAll(selector.child, parent)
                }
            }
            is Selector.Sibling -> {
                // Find all nodes whose parent contains both anchor and sibling
                val anchors = findAll(selector.anchor, root).toSet()
                findAll(selector.sibling, root).filter { sibling ->
                    // sibling is valid if it shares a parent with an anchor node
                    hasCommonParent(sibling, anchors, root)
                }
            }
            is Selector.Compound -> {
                val instance = selector.parts.filterIsInstance<Selector.Instance>().firstOrNull()
                val otherParts = selector.parts.filter { it !is Selector.Instance }
                val candidates = dfs(root).filter { node -> otherParts.all { part -> matchesSingle(part, node) } }
                if (instance != null) candidates.drop(instance.n).take(1) else candidates
            }
            else -> dfs(root).filter { matchesSingle(selector, it) }
        }

    private fun matchesSingle(selector: Selector, node: SerializedNode): Boolean = when (selector) {
        is Selector.Text -> node.text == selector.value
        is Selector.TextContains -> node.text?.contains(selector.value) == true
        is Selector.TextMatches -> node.text?.matches(Regex(selector.pattern)) == true
        is Selector.ResourceId -> {
            // Match short id (e.g. "btnLogin") or full id (e.g. "com.example:id/btnLogin")
            node.resourceId == selector.id || node.resourceId?.endsWith("/${selector.id}") == true
        }
        is Selector.Description -> node.contentDescription == selector.value
        is Selector.ClassName -> node.className == selector.name
        is Selector.Package -> node.packageName == selector.name
        is Selector.Clickable -> node.isClickable == selector.value
        is Selector.Enabled -> node.isEnabled == selector.value
        is Selector.Index -> node.indexInParent == selector.index
        is Selector.Instance -> true // handled in findAll
        is Selector.XPath -> throw UnsupportedOperationException(
            "XPath selector is not supported in the accessibility backend"
        )
        is Selector.Child, is Selector.Sibling, is Selector.Compound -> false // structural — not leaf-matchable
    }

    private fun dfs(node: SerializedNode): List<SerializedNode> {
        val result = mutableListOf(node)
        node.children.forEach { result.addAll(dfs(it)) }
        return result
    }

    private fun hasCommonParent(
        target: SerializedNode,
        anchors: Set<SerializedNode>,
        root: SerializedNode,
    ): Boolean {
        fun findParent(node: SerializedNode, child: SerializedNode): SerializedNode? {
            if (child in node.children) return node
            return node.children.firstNotNullOfOrNull { findParent(it, child) }
        }
        val parent = findParent(root, target) ?: return false
        return anchors.any { anchor -> anchor in parent.children }
    }
}
