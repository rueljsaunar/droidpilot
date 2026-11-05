package com.droidpilot.feature.recorder

import androidx.lifecycle.ViewModel
import com.droidpilot.core.automation.ActionRecorder
import com.droidpilot.core.automation.RecordedStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
public class RecorderViewModel @Inject constructor(
    private val recorder: ActionRecorder,
) : ViewModel() {

    public val isRecording: StateFlow<Boolean> = recorder.isRecording
    public val steps: StateFlow<List<RecordedStep>> = recorder.steps

    public fun startRecording(): Unit = recorder.startRecording()
    public fun stopRecording(): Unit = recorder.stopRecording()
    public fun clearSteps(): Unit = recorder.clearSteps()

    public fun updateStep(index: Int, code: String): Unit = recorder.updateStep(index, code)
    public fun deleteStep(index: Int): Unit = recorder.deleteStep(index)

    public fun generatePythonBlock(): String =
        steps.value.joinToString("\n") { it.code }

    override fun onCleared() {
        // Don't stop recording on VM cleared — recording outlives the screen
    }
}
