class DroidPilotError(Exception):
    """Base class for all droidpilot automation errors."""


class ElementNotFoundError(DroidPilotError):
    """Raised when a selector matches no element within the timeout."""


class TimeoutError(DroidPilotError):
    """Raised when a wait condition is not met within the timeout."""


class AssertionError(DroidPilotError):
    """Raised when an expect() assertion fails."""
