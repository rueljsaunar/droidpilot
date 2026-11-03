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
