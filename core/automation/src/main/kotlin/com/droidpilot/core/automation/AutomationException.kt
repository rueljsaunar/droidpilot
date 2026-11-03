package com.droidpilot.core.automation

public sealed class AutomationException(message: String) : Exception(message)

public class ElementNotFoundError(public val selector: Selector) :
    AutomationException("Element not found: $selector")

public class BackendUnavailableError(public val backend: String, public val setupHint: String) :
    AutomationException("Backend '$backend' unavailable. $setupHint")

public class ElementStaleError(public val selector: Selector) :
    AutomationException("Element became stale: $selector")

public class TargetCrashedError(public val packageName: String) :
    AutomationException("Target app '$packageName' crashed or disappeared")

public class TimeoutError(public val selector: Selector, public val timeoutMs: Long) :
    AutomationException("Timed out after ${timeoutMs}ms waiting for: $selector")
