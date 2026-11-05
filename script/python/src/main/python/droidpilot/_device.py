from com.droidpilot.script.python import DeviceProxy as _Proxy
from ._element import Element, _wrap_java_exception


class Device:
    """Entry point for all automation actions on the device."""

    @_wrap_java_exception
    def find(self, selector):
        """Find the first element matching selector; returns Element."""
        return Element(_Proxy.find(selector))

    @_wrap_java_exception
    def find_all(self, selector):
        """Find all elements matching selector; returns list of Element.

        The list is built by counting matching nodes once, then wrapping each by
        index. If the screen changes after the count is taken, an element at a
        given index may refer to a different node or raise ElementNotFoundError.
        """
        return [Element(p) for p in _Proxy.findAll(selector)]

    @_wrap_java_exception
    def tap(self, x, y):
        """Tap at screen coordinates (x, y)."""
        _Proxy.tap(float(x), float(y))

    @_wrap_java_exception
    def swipe(self, sx, sy, ex, ey, duration_ms=300):
        """Swipe from (sx, sy) to (ex, ey) over duration_ms milliseconds."""
        _Proxy.swipe(float(sx), float(sy), float(ex), float(ey), duration_ms)

    @_wrap_java_exception
    def drag(self, sx, sy, ex, ey, duration_ms=500):
        """Drag from (sx, sy) to (ex, ey) over duration_ms milliseconds."""
        _Proxy.drag(float(sx), float(sy), float(ex), float(ey), duration_ms)

    @_wrap_java_exception
    def pinch(self, cx, cy, scale):
        """Pinch at (cx, cy) with the given scale factor (< 1 = pinch in)."""
        _Proxy.pinch(float(cx), float(cy), float(scale))

    @_wrap_java_exception
    def press(self, key):
        """Press a hardware key: HOME, BACK, or RECENTS."""
        _Proxy.press(str(key))

    @_wrap_java_exception
    def app_start(self, pkg, activity=None):
        """Launch the app identified by package name.

        The accessibility backend resolves the launch intent via
        getLaunchIntentForPackage(), which picks the default MAIN/LAUNCHER
        activity. Specifying a custom activity is not yet supported; pass
        activity=None or omit it.
        """
        if activity is not None:
            raise NotImplementedError(
                "Specifying a custom activity is not yet supported by the accessibility backend."
            )
        _Proxy.appStart(pkg, None)

    @_wrap_java_exception
    def window_size(self):
        """Return (width, height) of the display in pixels."""
        arr = _Proxy.windowSize()
        return (arr[0], arr[1])

    @_wrap_java_exception
    def current_app(self):
        """Return the package name of the foreground app, or None."""
        return _Proxy.currentApp()

    @_wrap_java_exception
    def app_stop(self, pkg):
        """Force-stop the app identified by package name (requires Shizuku)."""
        _Proxy.appStop(pkg)

    @_wrap_java_exception
    def app_clear(self, pkg):
        """Clear app data and cache (requires Shizuku)."""
        _Proxy.appClear(pkg)

    @_wrap_java_exception
    def app_install(self, apk_path):
        """Install an APK from the given path (requires Shizuku)."""
        _Proxy.appInstall(apk_path)

    @_wrap_java_exception
    def shell(self, command):
        """Run a shell command and return a dict with 'stdout', 'stderr', 'exit_code' (requires Shizuku)."""
        result = _Proxy.shell(command)
        return {'stdout': result.stdout, 'stderr': result.stderr, 'exit_code': result.exitCode}

    @_wrap_java_exception
    def screencap(self):
        """Capture the screen and return JPEG bytes (requires Shizuku)."""
        return bytes(_Proxy.screencap())

    @_wrap_java_exception
    def logcat(self):
        """Return the last 500 lines of logcat output as a string (requires Shizuku)."""
        return _Proxy.logcat()

    @_wrap_java_exception
    def repo(self, name):
        """Look up a saved selector by friendly name from the Object Repository.

        Returns an Element ready to interact with. Raises DroidPilotError if no
        entry with the given name exists.

        Example::

            d.repo("Login button").click()
        """
        return Element(_Proxy.repo(str(name)))
