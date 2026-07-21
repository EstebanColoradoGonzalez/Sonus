package com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.estebancoloradogonzalez.sonus.core.domain.usecase.AddSourceFolderUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.DetectSourceFolderOverlapUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.GetSourceFolderRemovalImpactUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveSourceFoldersUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.RemoveSourceFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the post-onboarding Source Folders management screen (US-005).
 *
 * Holds no business rule nor I/O: it observes the registered folders through
 * [ObserveSourceFoldersUseCase] and delegates the add to [AddSourceFolderUseCase], which stays
 * unchanged from US-002 (it enforces exact-duplicate uniqueness and takes the SAF permission).
 *
 * The story's new rule is overlap: [DetectSourceFolderOverlapUseCase] is evaluated against the
 * folders already registered BEFORE the add, so a folder nested inside (or containing) another is
 * still registered but reported as an overlap warning (AC4). Every successful add marks pending scan
 * content (AC6); the scan itself is US-007.
 */
@HiltViewModel
class SettingsSourceFoldersViewModel
    @Inject
    constructor(
        observeSourceFolders: ObserveSourceFoldersUseCase,
        private val detectSourceFolderOverlap: DetectSourceFolderOverlapUseCase,
        private val addSourceFolder: AddSourceFolderUseCase,
        private val getRemovalImpact: GetSourceFolderRemovalImpactUseCase,
        private val removeSourceFolder: RemoveSourceFolderUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsSourceFoldersUiState())
        val uiState: StateFlow<SettingsSourceFoldersUiState> = _uiState.asStateFlow()

        private val _events = Channel<SettingsSourceFoldersEvent>(Channel.BUFFERED)
        val events: Flow<SettingsSourceFoldersEvent> = _events.receiveAsFlow()

        init {
            viewModelScope.launch {
                observeSourceFolders().collect { folders ->
                    _uiState.update { state ->
                        state.copy(
                            folders = folders.map { folder -> SourceFolderUi(folder.id, folder.displayPath) },
                        )
                    }
                }
            }
        }

        fun onCommand(command: SettingsSourceFoldersCommand) =
            when (command) {
                SettingsSourceFoldersCommand.AddFolderClicked ->
                    emit(SettingsSourceFoldersEvent.LaunchFolderPicker)
                is SettingsSourceFoldersCommand.FolderPicked -> addFolder(command.treeUri)
                SettingsSourceFoldersCommand.SelectionCancelled ->
                    emit(SettingsSourceFoldersEvent.NotifySelectionCancelled)
                is SettingsSourceFoldersCommand.RemoveFolderClicked ->
                    requestRemoval(command.id, command.displayPath)
                SettingsSourceFoldersCommand.RemoveFolderConfirmed -> confirmRemoval()
                SettingsSourceFoldersCommand.RemoveFolderDismissed ->
                    _uiState.update { it.copy(pendingRemoval = null) }
            }

        private fun addFolder(treeUri: String) {
            viewModelScope.launch {
                // Overlap is checked before the add so it is compared only against pre-existing folders.
                val overlaps = detectSourceFolderOverlap(treeUri)
                when (val result = addSourceFolder(LibraryCommand.AddSourceFolder(treeUri))) {
                    is OperationResult.Success -> {
                        _uiState.update { it.copy(hasPendingScanContent = true) }
                        emit(
                            if (overlaps) {
                                SettingsSourceFoldersEvent.NotifyOverlap
                            } else {
                                SettingsSourceFoldersEvent.NotifyFolderAdded
                            },
                        )
                    }
                    is OperationResult.Failure -> emit(result.error.toNotice())
                }
            }
        }

        private fun requestRemoval(
            id: Long,
            displayPath: String,
        ) {
            viewModelScope.launch {
                // Impact drives an informed decision (Invariant 3/5); isLast is read from the observed list.
                val trackCount = getRemovalImpact(id)
                _uiState.update { state ->
                    state.copy(
                        pendingRemoval =
                            PendingRemovalUi(
                                id = id,
                                displayPath = displayPath,
                                trackCount = trackCount,
                                isLastFolder = state.folders.size == 1,
                            ),
                    )
                }
            }
        }

        private fun confirmRemoval() {
            val pending = _uiState.value.pendingRemoval ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(pendingRemoval = null) }
                when (removeSourceFolder(LibraryCommand.RemoveSourceFolder(pending.id))) {
                    is OperationResult.Success -> emit(SettingsSourceFoldersEvent.NotifyFolderRemoved)
                    is OperationResult.Failure -> emit(SettingsSourceFoldersEvent.NotifyRemoveFailed)
                }
            }
        }

        private fun DomainError.toNotice(): SettingsSourceFoldersEvent =
            when (this) {
                DomainError.DuplicateSourceFolder -> SettingsSourceFoldersEvent.NotifyDuplicate
                DomainError.PermissionDenied -> SettingsSourceFoldersEvent.NotifyPermissionDenied
                is DomainError.EntityNotFound -> SettingsSourceFoldersEvent.NotifyPermissionDenied
                // Scan/settings failures cannot arise from add-folder; mapped defensively (no else).
                DomainError.PermissionRevoked -> SettingsSourceFoldersEvent.NotifyPermissionDenied
                is DomainError.ScanAborted -> SettingsSourceFoldersEvent.NotifyPermissionDenied
                DomainError.SettingsPersistenceFailed -> SettingsSourceFoldersEvent.NotifyPermissionDenied
            }

        private fun emit(event: SettingsSourceFoldersEvent) {
            _events.trySend(event)
        }
    }
