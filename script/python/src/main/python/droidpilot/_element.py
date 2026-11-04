import functools

from ._errors import DroidPilotError, ElementNotFoundError
from ._errors import TimeoutError as _TimeoutError


def _wrap_java_exception(func):
    """Re-raise Java exceptions as Python DroidPilotError subclasses."""
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except Exception as e:
            cause_cls = type(e).__name__
            if "ElementNotFoundError" in cause_cls:
                raise ElementNotFoundError(str(e)) from e
            if "TimeoutError" in cause_cls:
                raise _TimeoutError(str(e)) from e
            # Catch any Java/Kotlin exception that crossed the bridge:
            # AutomationException, BackendUnavailableError, IllegalArgumentException,
            # IllegalStateException, RuntimeException, etc.
            if "Exception" in cause_cls or "Error" in cause_cls:
                raise DroidPilotError(str(e)) from e
            raise
    return wrapper


class Element:
    """Wraps a UiElementProxy Java object returned by DeviceProxy."""

    def __init__(self, proxy):
        self._proxy = proxy

    @_wrap_java_exception
    def click(self):
        """Tap the element."""
        self._proxy.click()
        return self

    @_wrap_java_exception
    def long_click(self):
        """Long-press the element."""
        self._proxy.longClick()
        return self

    @_wrap_java_exception
    def set_text(self, text):
        """Clear existing text and type text."""
        self._proxy.setText(text)
        return self

    @_wrap_java_exception
    def clear_text(self):
        """Clear the element's text field."""
        self._proxy.clearText()
        return self

    @_wrap_java_exception
    def get_text(self):
        """Return the element's text, or None if absent."""
        return self._proxy.getText()

    @_wrap_java_exception
    def exists(self):
        """Return True if the element is present in the current UI tree."""
        return bool(self._proxy.exists())

    @_wrap_java_exception
    def wait(self, timeout=5000):
        """Block until the element exists; raises TimeoutError if it does not appear."""
        self._proxy.wait(timeout)
        return self

    @_wrap_java_exception
    def wait_gone(self, timeout=5000):
        """Block until the element disappears; return True if gone within timeout."""
        return bool(self._proxy.waitGone(timeout))

    @_wrap_java_exception
    def info(self):
        """Return a dict of element attributes (text, enabled, clickable, password, …)."""
        return dict(self._proxy.info())
