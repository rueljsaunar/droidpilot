package com.droidpilot.app.ui

import androidx.lifecycle.ViewModel
import com.droidpilot.backend.shizuku.ShizukuRepository
import com.droidpilot.backend.shizuku.ShizukuState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ShizukuViewModel @Inject constructor(
    private val repository: ShizukuRepository,
) : ViewModel() {

    val state: StateFlow<ShizukuState> = repository.state

    fun requestPermission() {
        repository.requestPermission(ShizukuRepository.PERMISSION_REQUEST_CODE)
    }
}
