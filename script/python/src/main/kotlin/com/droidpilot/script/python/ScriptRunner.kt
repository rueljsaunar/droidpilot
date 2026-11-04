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
        Python.getInstance().getModule("builtins").callAttr("exec", script)
    }
}
