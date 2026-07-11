package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.estebancoloradogonzalez.sonus.feature.settings.R
import kotlinx.coroutines.launch

/**
 * Source Folders selection screen of the first-run flow (US-002). Lets the Listener add one or more
 * directories via the SAF `OpenDocumentTree` picker, review and remove them, and continue to the
 * foundational scan once at least one folder is registered.
 *
 * The screen owns the SAF picker launcher (the only place the picker is triggered); all flow
 * decisions belong to the [SourceFoldersViewModel]. SAF permission handling lives in the data layer.
 */
@Composable
fun SourceFoldersScreen(
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SourceFoldersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val duplicateNotice = stringResource(R.string.source_folders_duplicate_notice)
    val permissionDeniedNotice = stringResource(R.string.source_folders_permission_denied_notice)
    val cancelledNotice = stringResource(R.string.source_folders_selection_cancelled_notice)

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri == null) {
                viewModel.onCommand(SourceFoldersCommand.SelectionCancelled)
            } else {
                viewModel.onCommand(SourceFoldersCommand.FolderPicked(uri.toString()))
            }
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SourceFoldersEvent.LaunchFolderPicker -> folderPickerLauncher.launch(null)
                SourceFoldersEvent.NavigateToScan -> onNavigateToScan()
                SourceFoldersEvent.NotifyDuplicate ->
                    scope.launch { snackbarHostState.showSnackbar(duplicateNotice) }
                SourceFoldersEvent.NotifyPermissionDenied ->
                    scope.launch { snackbarHostState.showSnackbar(permissionDeniedNotice) }
                SourceFoldersEvent.NotifySelectionCancelled ->
                    scope.launch { snackbarHostState.showSnackbar(cancelledNotice) }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        SourceFoldersContent(
            padding = innerPadding,
            uiState = uiState,
            onAddFolder = { viewModel.onCommand(SourceFoldersCommand.AddFolderClicked) },
            onRemoveFolder = { id -> viewModel.onCommand(SourceFoldersCommand.RemoveFolder(id)) },
            onContinue = { viewModel.onCommand(SourceFoldersCommand.ContinueClicked) },
        )
    }
}

@Composable
private fun SourceFoldersContent(
    padding: PaddingValues,
    uiState: SourceFoldersUiState,
    onAddFolder: () -> Unit,
    onRemoveFolder: (Long) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.source_folders_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.source_folders_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
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
                    text = stringResource(R.string.source_folders_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = uiState.folders, key = { it.id }) { folder ->
                        SourceFolderRow(folder = folder, onRemove = { onRemoveFolder(folder.id) })
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onAddFolder,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.source_folders_add))
        }
        Button(
            onClick = onContinue,
            enabled = uiState.canContinue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
        ) {
            Text(text = stringResource(R.string.source_folders_continue))
        }
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
            Text(text = stringResource(R.string.source_folders_remove))
        }
    }
}
