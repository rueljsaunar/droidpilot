package com.droidpilot.runner

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class RunRepository @Inject constructor() {
    private val _events = MutableSharedFlow<RunEvent>(replay = 100)
    public val events: SharedFlow<RunEvent> = _events.asSharedFlow()

    private val _isRunning = MutableStateFlow(false)
    public val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentStep = MutableStateFlow("")
    public val currentStep: StateFlow<String> = _currentStep.asStateFlow()

    private val _passCount = MutableStateFlow(0)
    public val passCount: StateFlow<Int> = _passCount.asStateFlow()

    private val _failCount = MutableStateFlow(0)
    public val failCount: StateFlow<Int> = _failCount.asStateFlow()

    public suspend fun emit(event: RunEvent) {
        _events.emit(event)
        when (event) {
            is RunEvent.TestStarted -> {
                _isRunning.value = true
                _currentStep.value = event.testCase.functionName
            }
            is RunEvent.TestPassed -> _passCount.value++
            is RunEvent.TestFailed -> _failCount.value++
            is RunEvent.RunComplete -> {
                _isRunning.value = false
                _currentStep.value = ""
            }
            is RunEvent.StepLogged -> { /* no aggregate state change */ }
        }
    }

    public fun reset() {
        _isRunning.value = false
        _currentStep.value = ""
        _passCount.value = 0
        _failCount.value = 0
    }
}
