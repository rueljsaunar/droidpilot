package com.droidpilot.core.automation

import kotlinx.coroutines.flow.StateFlow

public interface ActionRecorder {
    public val isRecording: StateFlow<Boolean>
    public val steps: StateFlow<List<RecordedStep>>
    public fun startRecording()
    public fun stopRecording()
    public fun clearSteps()
    public fun updateStep(index: Int, code: String)
    public fun deleteStep(index: Int)
}
