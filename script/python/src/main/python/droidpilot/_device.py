from com.droidpilot.script.python import DeviceProxy as _Proxy
from ._element import Element


class Device:
    """Entry point for all automation actions on the device."""

    def find(self, selector):
        """Find the first element matching selector; returns Element."""
        return Element(_Proxy.find(selector))

    def find_all(self, selector):
        """Find all elements matching selector; returns list of Element."""
        return [Element(p) for p in _Proxy.findAll(selector)]

    def tap(self, x, y):
        """Tap at screen coordinates (x, y)."""
        _Proxy.tap(float(x), float(y))

    def swipe(self, sx, sy, ex, ey, duration_ms=300):
        """Swipe from (sx, sy) to (ex, ey) over duration_ms milliseconds."""
        _Proxy.swipe(float(sx), float(sy), float(ex), float(ey), duration_ms)

    def drag(self, sx, sy, ex, ey, duration_ms=500):
        """Drag from (sx, sy) to (ex, ey) over duration_ms milliseconds."""
        _Proxy.drag(float(sx), float(sy), float(ex), float(ey), duration_ms)

    def pinch(self, cx, cy, scale):
        """Pinch at (cx, cy) with the given scale factor (< 1 = pinch in)."""
        _Proxy.pinch(float(cx), float(cy), float(scale))

    def press(self, key):
        """Press a hardware key: HOME, BACK, or RECENTS."""
        _Proxy.press(str(key))

    def app_start(self, pkg, activity=None):
        """Launch the app identified by package name, optionally specifying activity."""
        _Proxy.appStart(pkg, activity)

    def window_size(self):
        """Return (width, height) of the display in pixels."""
        arr = _Proxy.windowSize()
        return (arr[0], arr[1])

    def current_app(self):
        """Return the package name of the foreground app, or None."""
        return _Proxy.currentApp()
