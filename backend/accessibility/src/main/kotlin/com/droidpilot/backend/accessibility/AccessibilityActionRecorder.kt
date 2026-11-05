package com.droidpilot.backend.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.droidpilot.core.automation.ActionRecorder
import com.droidpilot.core.automation.RecordedStep
import com.droidpilot.core.automation.Selector
import com.droidpilot.core.automation.toPythonExpression
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AccessibilityActionRecorder @Inject constructor() : ActionRecorder {

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _steps = MutableStateFlow<List<RecordedStep>>(emptyList())
    override val steps: StateFlow<List<RecordedStep>> = _steps.asStateFlow()

    override fun startRecording() {
        _steps.value = emptyList()
        _isRecording.value = true
        DroidPilotAccessibilityService.recorder = this
    }

    override fun stopRecording() {
        _isRecording.value = false
        DroidPilotAccessibilityService.recorder = null
    }

    override fun clearSteps() {
        _steps.value = emptyList()
    }

    override fun updateStep(index: Int, code: String) {
        _steps.update { list -> list.map { if (it.index == index) it.copy(code = code) else it } }
    }

    override fun deleteStep(index: Int) {
        _steps.update { list ->
            list.filter { it.index != index }.mapIndexed { i, s -> s.copy(index = i) }
        }
    }

    internal fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!_isRecording.value) return
        val step = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED     -> buildClickStep(event)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> buildTextStep(event)
            else                                      -> null
        } ?: return
        _steps.update { it + step.copy(index = it.size) }
    }

    private fun buildClickStep(event: AccessibilityEvent): RecordedStep {
        val source = event.source
        return if (source != null) {
            val selector = bestSelector(source)
            source.recycle()
            if (selector != null) {
                RecordedStep(0, "${selector.toPythonExpression()}.click()", "click ${describeSelector(selector)}")
            } else {
                val bounds = nodeBounds(event.source)
                val cx = bounds?.centerX() ?: 0
                val cy = bounds?.centerY() ?: 0
                RecordedStep(0, "d.tap($cx, $cy)", "tap ($cx, $cy)")
            }
        } else {
            RecordedStep(0, "# tap (element not identified)", "unidentified tap")
        }
    }

    private fun buildTextStep(event: AccessibilityEvent): RecordedStep? {
        val source = event.source ?: return null
        val selector = bestSelector(source)
        val text = event.text.joinToString("").replace("\"", "\\\"")
        source.recycle()
        return if (selector != null) {
            RecordedStep(0, "${selector.toPythonExpression()}.set_text(\"$text\")", "set_text \"$text\" on ${describeSelector(selector)}")
        } else {
            RecordedStep(0, "# set_text \"$text\" (element not identified)", "set_text (no selector)")
        }
    }

    private fun bestSelector(node: AccessibilityNodeInfo): Selector? {
        node.viewIdResourceName?.takeIf { it.isNotBlank() }?.let { return Selector.ResourceId(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { return Selector.Description(it) }
        node.text?.toString()?.takeIf { it.isNotBlank() && !node.isPassword }?.let { return Selector.Text(it) }
        return null
    }

    private fun nodeBounds(node: AccessibilityNodeInfo?): Rect? {
        node ?: return null
        val r = Rect()
        node.getBoundsInScreen(r)
        return r
    }

    private fun describeSelector(selector: Selector): String = when (selector) {
        is Selector.ResourceId  -> selector.id.substringAfterLast('/')
        is Selector.Text        -> "\"${selector.value.take(20)}\""
        is Selector.Description -> "\"${selector.value.take(20)}\""
        else                    -> selector.toString()
    }
}
