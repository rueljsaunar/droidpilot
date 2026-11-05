from ._errors import AssertionError as _AssertionError
from ._errors import TimeoutError as _TimeoutError


class Expectation:
    """Fluent assertion wrapper returned by expect()."""

    def __init__(self, element):
        self._element = element

    def to_be_visible(self, timeout=5000):
        """Assert the element appears within timeout ms."""
        try:
            self._element.wait(timeout)
        except _TimeoutError as e:
            raise _AssertionError(
                f"Expected element to be visible within {timeout}ms"
            ) from e
        if not self._element.exists():
            raise _AssertionError("Expected element to be visible but exists() returned False")
        return self

    def to_be_gone(self, timeout=5000):
        """Assert the element disappears within timeout ms."""
        if not self._element.wait_gone(timeout):
            raise _AssertionError(f"Expected element to be gone within {timeout}ms")
        return self

    def to_have_text(self, expected):
        """Assert the element's text equals expected."""
        actual = self._element.get_text()
        if actual != expected:
            raise _AssertionError(f"Expected text '{expected}', got '{actual}'")
        return self

    def to_be_enabled(self):
        """Assert the element's enabled attribute is True."""
        if not self._element.info().get("enabled"):
            raise _AssertionError("Expected element to be enabled")
        return self


def expect(element):
    """Return an Expectation for fluent assertions on element."""
    return Expectation(element)
