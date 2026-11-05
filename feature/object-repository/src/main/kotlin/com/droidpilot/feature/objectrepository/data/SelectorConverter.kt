package com.droidpilot.feature.objectrepository.data

import com.droidpilot.core.automation.Selector

/** Simple pipe-delimited serialization for single-type selectors. */
internal object SelectorConverter {

    fun serialize(selector: Selector): String = when (selector) {
        is Selector.ResourceId     -> "ResourceId|${selector.id}"
        is Selector.Text           -> "Text|${selector.value}"
        is Selector.TextContains   -> "TextContains|${selector.value}"
        is Selector.TextMatches    -> "TextMatches|${selector.pattern}"
        is Selector.Description    -> "Description|${selector.value}"
        is Selector.ClassName      -> "ClassName|${selector.name}"
        is Selector.Package        -> "Package|${selector.name}"
        is Selector.Clickable      -> "Clickable|${selector.value}"
        is Selector.Enabled        -> "Enabled|${selector.value}"
        is Selector.Index          -> "Index|${selector.index}"
        is Selector.Instance       -> "Instance|${selector.n}"
        is Selector.XPath          -> "XPath|${selector.expression}"
        is Selector.Child          -> "Child|${serialize(selector.parent)}~~${serialize(selector.child)}"
        is Selector.Sibling        -> "Sibling|${serialize(selector.anchor)}~~${serialize(selector.sibling)}"
        is Selector.Compound       -> "Compound|${selector.parts.joinToString("~~") { serialize(it) }}"
    }

    fun deserialize(raw: String): Selector {
        val idx = raw.indexOf('|')
        if (idx < 0) return Selector.Text(raw)
        val type = raw.substring(0, idx)
        val value = raw.substring(idx + 1)
        return when (type) {
            "ResourceId"   -> Selector.ResourceId(value)
            "Text"         -> Selector.Text(value)
            "TextContains" -> Selector.TextContains(value)
            "TextMatches"  -> Selector.TextMatches(value)
            "Description"  -> Selector.Description(value)
            "ClassName"    -> Selector.ClassName(value)
            "Package"      -> Selector.Package(value)
            "Clickable"    -> Selector.Clickable(value.toBoolean())
            "Enabled"      -> Selector.Enabled(value.toBoolean())
            "Index"        -> Selector.Index(value.toInt())
            "Instance"     -> Selector.Instance(value.toInt())
            "XPath"        -> Selector.XPath(value)
            "Child"        -> {
                val parts = value.split("~~", limit = 2)
                Selector.Child(deserialize(parts[0]), deserialize(parts[1]))
            }
            "Sibling"      -> {
                val parts = value.split("~~", limit = 2)
                Selector.Sibling(deserialize(parts[0]), deserialize(parts[1]))
            }
            "Compound"     -> Selector.Compound(value.split("~~").map { deserialize(it) })
            else           -> Selector.Text(value)
        }
    }
}
