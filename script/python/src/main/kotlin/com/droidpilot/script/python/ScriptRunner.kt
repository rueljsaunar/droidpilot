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

    /**
     * Executes [script] and returns all stdout/stderr output as a string.
     * stdout and stderr are redirected to a StringIO buffer during execution.
     * Always executed on the calling thread — callers must use Dispatchers.IO.
     */
    public fun executeCapturingOutput(script: String): String {
        check(Python.isStarted()) {
            "Python interpreter not started — call Python.start() first"
        }
        val py = Python.getInstance()
        val builtins = py.getModule("builtins")
        val globals = builtins.callAttr("dict")
        globals.callAttr("__setitem__", "__name__", "__main__")

        val setup = "import io as __io__, sys as __sys__\n" +
            "__cap__ = __io__.StringIO()\n" +
            "__sys__.stdout = __cap__\n" +
            "__sys__.stderr = __cap__"
        builtins.callAttr("exec", setup, globals)

        try {
            builtins.callAttr("exec", script, globals)
        } catch (e: Exception) {
            val errMsg = e.message ?: e.toString()
            val writeErr = "__cap__.write(${errMsg.pythonStringLiteral()})"
            runCatching { builtins.callAttr("exec", writeErr, globals) }
        } finally {
            val restore = "__sys__.stdout = __sys__.__stdout__\n" +
                "__sys__.stderr = __sys__.__stderr__\n" +
                "__output__ = __cap__.getvalue()"
            builtins.callAttr("exec", restore, globals)
        }
        return globals.callAttr("get", "__output__")?.toString() ?: ""
    }

    private fun String.pythonStringLiteral(): String {
        val escaped = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return "\"$escaped\""
    }
}
