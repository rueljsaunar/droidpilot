package com.droidpilot.feature.projects.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidpilot.reporting.ReportRepository
import com.droidpilot.reporting.RunRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

@HiltViewModel
public class RunHistoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    reportRepository: ReportRepository,
) : ViewModel() {

    private val projectId: Long = savedState["projectId"] ?: -1L

    public val runs: StateFlow<List<RunRecord>> = reportRepository
        .getRunsForProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    public fun buildShareIntent(context: Context, run: RunRecord): Intent? {
        val path = run.htmlReportPath ?: run.xmlReportPath ?: return null
        val file = File(path)
        if (!file.exists()) return null
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val mimeType = if (path.endsWith(".html")) "text/html" else "text/xml"
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
