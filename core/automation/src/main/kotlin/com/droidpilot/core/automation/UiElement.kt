package com.droidpilot.core.automation

public interface UiElement {
    public suspend fun click()
    public suspend fun longClick()
    public suspend fun setText(text: String)
    public suspend fun clearText()
    /** Returns "[REDACTED]" for password fields. */
    public suspend fun getText(): String?
    public suspend fun exists(): Boolean
    public suspend fun wait(timeoutMs: Long = 5_000): UiElement
    public suspend fun waitGone(timeoutMs: Long = 5_000): Boolean
    public fun bounds(): Bounds
    public fun info(): ElementInfo
    public suspend fun scrollTo(selector: Selector): UiElement
    public fun child(selector: Selector): UiElement
    public fun sibling(selector: Selector): UiElement
}
