package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.sonus.feature.library.R

/**
 * Global scan-progress overlay (US-009). Presented over the main navigation while any scan is active,
 * it observes the scan-state channel (C2) through [ScanProgressViewModel] and renders the current
 * phase: deterministic/indeterminate progress (AC3/AC4), the catalog-sync activity (AC5), the
 * four-counter summary on completion (AC6) and the abort recovery (AC7).
 *
 * The overlay is a full-screen scrim that intercepts every pointer event, so the underlying
 * navigation is blocked while it is shown (AC2). When the phase is [ScanProgressPhase.HIDDEN] it
 * renders nothing, so it never interferes with normal navigation (AC1).
 */
@Composable
fun ScanProgressOverlay(
    onGoToLibrary: () -> Unit,
    onConfigureFolders: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ScanProgressOverlayContent(
        uiState = uiState,
        onRetry = { viewModel.onCommand(ScanProgressCommand.Retry) },
        onGoToLibrary = {
            viewModel.onCommand(ScanProgressCommand.Dismiss)
            onGoToLibrary()
        },
        onConfigureFolders = {
            viewModel.onCommand(ScanProgressCommand.Dismiss)
            onConfigureFolders()
        },
        modifier = modifier,
    )
}

@Composable
private fun ScanProgressOverlayContent(
    uiState: ScanProgressUiState,
    onRetry: () -> Unit,
    onGoToLibrary: () -> Unit,
    onConfigureFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.phase == ScanProgressPhase.HIDDEN) {
        return
    }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                // Consume all gestures so navigation underneath is blocked while the scan runs (AC2).
                .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (uiState.phase) {
                    ScanProgressPhase.SCANNING -> ScanningBody(uiState)
                    ScanProgressPhase.SYNCING -> SyncingBody()
                    ScanProgressPhase.FINISHED -> FinishedBody(uiState.summary, onGoToLibrary)
                    ScanProgressPhase.ABORTED ->
                        AbortedBody(uiState.abortCode, onRetry, onConfigureFolders)
                    ScanProgressPhase.HIDDEN -> Unit
                }
            }
        }
    }
}

@Composable
private fun ScanningBody(uiState: ScanProgressUiState) {
    Text(
        text = stringResource(R.string.scan_overlay_scanning_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
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
            text = stringResource(R.string.scan_overlay_count, uiState.processed, total),
            style = MaterialTheme.typography.bodyMedium,
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
            text = stringResource(R.string.scan_overlay_indeterminate, uiState.processed),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SyncingBody() {
    Text(
        text = stringResource(R.string.scan_overlay_syncing_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
    Text(
        text = stringResource(R.string.scan_overlay_syncing_message),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun FinishedBody(
    summary: ScanResultUi?,
    onGoToLibrary: () -> Unit,
) {
    Text(
        text = stringResource(R.string.scan_overlay_finished_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    if (summary != null) {
        Text(
            text = stringResource(R.string.scan_overlay_added, summary.added),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.scan_overlay_purged, summary.purged),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.scan_overlay_unsupported, summary.unsupported),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.scan_overlay_orphan_dims, summary.orphanDimsPurged),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Button(
        onClick = onGoToLibrary,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
    ) {
        Text(text = stringResource(R.string.scan_overlay_go_to_library))
    }
}

@Composable
private fun AbortedBody(
    abortCode: String?,
    onRetry: () -> Unit,
    onConfigureFolders: () -> Unit,
) {
    Text(
        text = stringResource(R.string.scan_overlay_aborted_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    val causeRes =
        when (abortCode) {
            ABORT_CODE_PERMISSION_REVOKED -> R.string.scan_overlay_aborted_permission_revoked
            else -> R.string.scan_overlay_aborted_generic
        }
    Text(
        text = stringResource(causeRes),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 16.dp),
    )
    Button(
        onClick = onRetry,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
    ) {
        Text(text = stringResource(R.string.scan_overlay_retry))
    }
    OutlinedButton(
        onClick = onConfigureFolders,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    ) {
        Text(text = stringResource(R.string.scan_overlay_configure_folders))
    }
}

private const val SCRIM_ALPHA = 0.6f
private const val ABORT_CODE_PERMISSION_REVOKED = "ERR_PERMISSION_REVOKED"
