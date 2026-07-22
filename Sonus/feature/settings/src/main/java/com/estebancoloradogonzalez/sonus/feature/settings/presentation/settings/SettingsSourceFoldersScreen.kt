package com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.sonus.feature.settings.R
import kotlinx.coroutines.launch

/**
 * Post-onboarding Source Folders management screen (US-005). Lets the Listener add a directory via
 * the SAF `OpenDocumentTree` picker in normal operation, keeping the existing folders and Catalog
 * intact. Removing folders is US-006 and scanning is US-007, both out of scope here.
 *
 * The screen owns the SAF picker launcher (the only place the picker is triggered); all flow
 * decisions belong to the [SettingsSourceFoldersViewModel]. SAF permission handling lives in the
 * data layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSourceFoldersScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsSourceFoldersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri == null) {
                viewModel.onCommand(SettingsSourceFoldersCommand.SelectionCancelled)
            } else {
                viewModel.onCommand(SettingsSourceFoldersCommand.FolderPicked(uri.toString()))
            }
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val noticeRes = event.noticeRes()
            if (noticeRes == null) {
                folderPickerLauncher.launch(null)
            } else {
                scope.launch { snackbarHostState.showSnackbar(context.getString(noticeRes)) }
            }
        }
    }

    uiState.pendingRemoval?.let { pending ->
        RemoveFolderDialog(
            pending = pending,
            onConfirm = { viewModel.onCommand(SettingsSourceFoldersCommand.RemoveFolderConfirmed) },
            onDismiss = { viewModel.onCommand(SettingsSourceFoldersCommand.RemoveFolderDismissed) },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_source_folders_title)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = stringResource(R.string.settings_source_folders_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        SettingsSourceFoldersContent(
            padding = innerPadding,
            uiState = uiState,
            onAddFolder = { viewModel.onCommand(SettingsSourceFoldersCommand.AddFolderClicked) },
            onRemoveFolder = { folder ->
                viewModel.onCommand(SettingsSourceFoldersCommand.RemoveFolderClicked(folder.id, folder.displayPath))
            },
            onRescan = { viewModel.onCommand(SettingsSourceFoldersCommand.RescanClicked) },
        )
    }
}

/**
 * Resolves the string resource for a one-shot notice event, or `null` when the event is not a
 * snackbar notice ([SettingsSourceFoldersEvent.LaunchFolderPicker], handled separately). Exhaustive
 * `when` (no `else`) so a new event fails to compile until it is mapped.
 */
private fun SettingsSourceFoldersEvent.noticeRes(): Int? =
    when (this) {
        SettingsSourceFoldersEvent.LaunchFolderPicker -> null
        SettingsSourceFoldersEvent.NotifyFolderAdded -> R.string.settings_source_folders_added_notice
        SettingsSourceFoldersEvent.NotifyOverlap -> R.string.settings_source_folders_overlap_notice
        SettingsSourceFoldersEvent.NotifyDuplicate -> R.string.settings_source_folders_duplicate_notice
        SettingsSourceFoldersEvent.NotifyPermissionDenied -> R.string.settings_source_folders_permission_denied_notice
        SettingsSourceFoldersEvent.NotifySelectionCancelled -> R.string.settings_source_folders_cancelled_notice
        SettingsSourceFoldersEvent.NotifyFolderRemoved -> R.string.settings_source_folders_removed_notice
        SettingsSourceFoldersEvent.NotifyRemoveFailed -> R.string.settings_source_folders_remove_failed_notice
        SettingsSourceFoldersEvent.NotifyRescanStarted -> R.string.settings_source_folders_rescan_started
        SettingsSourceFoldersEvent.NotifyNoFoldersToScan -> R.string.settings_source_folders_rescan_no_folders
    }

@Composable
private fun RemoveFolderDialog(
    pending: PendingRemovalUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_source_folders_remove_title, pending.displayPath)) },
        text = {
            Column {
                Text(
                    text =
                        stringResource(
                            R.string.settings_source_folders_remove_impact,
                            pending.trackCount,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (pending.isLastFolder) {
                    Text(
                        text = stringResource(R.string.settings_source_folders_remove_last_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.settings_source_folders_remove_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_source_folders_remove_cancel))
            }
        },
    )
}

@Composable
private fun SettingsSourceFoldersContent(
    padding: PaddingValues,
    uiState: SettingsSourceFoldersUiState,
    onAddFolder: () -> Unit,
    onRemoveFolder: (SourceFolderUi) -> Unit,
    onRescan: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_source_folders_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (uiState.hasPendingScanContent) {
            PendingScanBanner(modifier = Modifier.padding(top = 16.dp))
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.folders.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_source_folders_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = uiState.folders, key = { it.id }) { folder ->
                        SourceFolderRow(folder = folder, onRemove = { onRemoveFolder(folder) })
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onAddFolder,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.settings_source_folders_add))
        }
        OutlinedButton(
            onClick = onRescan,
            enabled = uiState.folders.isNotEmpty(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
        ) {
            Text(text = stringResource(R.string.settings_source_folders_rescan_action))
        }
    }
}

@Composable
private fun PendingScanBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = stringResource(R.string.settings_source_folders_pending_scan),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun SourceFolderRow(
    folder: SourceFolderUi,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = folder.displayPath,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRemove) {
            Text(text = stringResource(R.string.settings_source_folders_remove))
        }
    }
}
