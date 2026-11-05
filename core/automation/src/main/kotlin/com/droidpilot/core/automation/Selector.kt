package com.droidpilot.core.automation

public sealed class Selector {
    public data class Text(val value: String) : Selector()
    public data class TextContains(val value: String) : Selector()
    public data class TextMatches(val pattern: String) : Selector()
    public data class ResourceId(val id: String) : Selector()
    public data class Description(val value: String) : Selector()
    public data class ClassName(val name: String) : Selector()
    public data class Package(val name: String) : Selector()
    public data class Clickable(val value: Boolean) : Selector()
    public data class Enabled(val value: Boolean) : Selector()
    public data class Index(val index: Int) : Selector()
    public data class Instance(val n: Int) : Selector()
    public data class XPath(val expression: String) : Selector()
    public data class Child(val parent: Selector, val child: Selector) : Selector()
    public data class Sibling(val anchor: Selector, val sibling: Selector) : Selector()
    public data class Compound(val parts: List<Selector>) : Selector()
}

public fun Selector.Compound.flatten(): List<Selector> =
    parts.flatMap { part ->
        if (part is Selector.Compound) part.flatten() else listOf(part)
    }

/** Python expression that evaluates to a UiElementProxy, e.g. `d.element(id="x")`. */
public fun Selector.toPythonExpression(): String = when (this) {
    is Selector.ResourceId   -> """d.element(id="${id.replace("\"", "\\\"")}")"""
    is Selector.Text         -> """d.element(text="${value.replace("\"", "\\\"")}")"""
    is Selector.Description  -> """d.element(desc="${value.replace("\"", "\\\"")}")"""
    is Selector.ClassName    -> """d.element(cls="${name.replace("\"", "\\\"")}")"""
    else                     -> "d.element()"
}

/** Human-readable representation suitable for display and clipboard. */
public fun Selector.toDisplayString(): String = when (this) {
    is Selector.ResourceId     -> "id:$id"
    is Selector.Text           -> "text:$value"
    is Selector.TextContains   -> "text_contains:$value"
    is Selector.TextMatches    -> "text_matches:$pattern"
    is Selector.Description    -> "desc:$value"
    is Selector.ClassName      -> "class:$name"
    is Selector.Package        -> "pkg:$name"
    is Selector.Clickable      -> "clickable:$value"
    is Selector.Enabled        -> "enabled:$value"
    is Selector.Index          -> "index:$index"
    is Selector.Instance       -> "instance:$n"
    is Selector.XPath          -> "xpath:$expression"
    is Selector.Child          -> "${parent.toDisplayString()} > ${child.toDisplayString()}"
    is Selector.Sibling        -> "${anchor.toDisplayString()} ~ ${sibling.toDisplayString()}"
    is Selector.Compound       -> parts.joinToString(" & ") { it.toDisplayString() }
}
