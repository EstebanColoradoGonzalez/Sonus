package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.sonus.feature.library.R
import kotlinx.coroutines.launch

/**
 * Foundational scan screen (US-003). Shows deterministic progress while the Library Engine builds
 * the catalog in the background, transitions automatically to the library on completion, and offers
 * recovery (retry / back to Source Folders) if the scan is aborted or cancelled.
 */
@Composable
fun ScanScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateBackToSourceFolders: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val abortedNotice = stringResource(R.string.scan_aborted_notice)
    val cancelledNotice = stringResource(R.string.scan_cancelled_notice)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ScanEvent.NavigateToLibrary -> onNavigateToLibrary()
                is ScanEvent.NotifyAborted ->
                    scope.launch { snackbarHostState.showSnackbar(abortedNotice) }
                ScanEvent.NotifyCancelled ->
                    scope.launch { snackbarHostState.showSnackbar(cancelledNotice) }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ScanContent(
            padding = innerPadding,
            uiState = uiState,
            onCancel = { viewModel.onCommand(ScanCommand.CancelScan) },
            onRetry = { viewModel.onCommand(ScanCommand.RetryScan) },
            onBackToSourceFolders = onNavigateBackToSourceFolders,
        )
    }
}

@Composable
private fun ScanContent(
    padding: PaddingValues,
    uiState: ScanUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onBackToSourceFolders: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState.status) {
            ScanStatus.STARTING, ScanStatus.SCANNING, ScanStatus.SYNCING ->
                ScanInProgress(uiState = uiState, onCancel = onCancel)
            ScanStatus.FINISHED ->
                ScanFinished(summary = uiState.summary)
            ScanStatus.ABORTED ->
                ScanAborted(onRetry = onRetry, onBackToSourceFolders = onBackToSourceFolders)
        }
    }
}

@Composable
private fun ScanInProgress(
    uiState: ScanUiState,
    onCancel: () -> Unit,
) {
    Text(
        text = stringResource(R.string.scan_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    val phase =
        when (uiState.status) {
            ScanStatus.SCANNING -> stringResource(R.string.scan_phase_scanning)
            ScanStatus.SYNCING -> stringResource(R.string.scan_phase_syncing)
            else -> stringResource(R.string.scan_phase_starting)
        }
    Text(
        text = phase,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    val total = uiState.total
    if (total != null && total > 0) {
        LinearProgressIndicator(
            progress = { uiState.processed.toFloat() / total.toFloat() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.scan_progress_count, uiState.processed, total),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    } else {
        LinearProgressIndicator(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.scan_progress_indeterminate, uiState.processed),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.padding(top = 24.dp),
    ) {
        Text(text = stringResource(R.string.scan_cancel))
    }
}

@Composable
private fun ScanFinished(summary: ScanSummaryUi?) {
    Text(
        text = stringResource(R.string.scan_finished_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    if (summary != null) {
        Text(
            text = stringResource(R.string.scan_summary_added, summary.added),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.scan_summary_unsupported, summary.unsupported),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ScanAborted(
    onRetry: () -> Unit,
    onBackToSourceFolders: () -> Unit,
) {
    Text(
        text = stringResource(R.string.scan_aborted_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Button(
        onClick = onRetry,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
    ) {
        Text(text = stringResource(R.string.scan_retry))
    }
    OutlinedButton(
        onClick = onBackToSourceFolders,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    ) {
        Text(text = stringResource(R.string.scan_back_to_folders))
    }
}
