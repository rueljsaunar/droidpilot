import builtins as _builtins


class DroidPilotError(Exception):
    """Base class for all droidpilot automation errors."""


class ElementNotFoundError(DroidPilotError):
    """Raised when a selector matches no element within the timeout."""


# Inherits from the built-in so that plain `except TimeoutError` still works.
class TimeoutError(DroidPilotError, _builtins.TimeoutError):
    """Raised when a wait condition is not met within the timeout."""


# Inherits from the built-in so that plain `except AssertionError` still works.
class AssertionError(DroidPilotError, _builtins.AssertionError):
    """Raised when an expect() assertion fails."""
