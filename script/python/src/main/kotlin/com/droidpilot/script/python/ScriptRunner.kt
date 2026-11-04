package com.droidpilot.script.python

import com.chaquo.python.Python

public object ScriptRunner {
    /**
     * Executes [script] in the Chaquopy Python interpreter.
     *
     * @throws IllegalStateException if [Python.start] was not called before this.
     * @throws RuntimeException wrapping any Python exception raised during execution.
     */
    public fun execute(script: String) {
        check(Python.isStarted()) {
            "Python interpreter not started — call Python.start() first"
        }
        val py = Python.getInstance()
        val builtins = py.getModule("builtins")
        // exec() called from Java has no enclosing Python frame, so globals and locals
        // would be NULL, raising SystemError. Supply an explicit globals dict with
        // __name__ = "__main__" so relative imports and dunder checks work as expected.
        val globals = builtins.callAttr("dict")
        globals.callAttr("__setitem__", "__name__", "__main__")
        builtins.callAttr("exec", script, globals)
    }
}
