package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.estebancoloradogonzalez.sonus.core.domain.usecase.AddSourceFolderUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveSourceFoldersUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.RemoveSourceFolderUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.VerifySourceFoldersReadyUseCase
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
 * Drives the Source Folders selection step of the first-run flow (US-002).
 *
 * Holds no business rule nor I/O: it observes the registered folders through
 * [ObserveSourceFoldersUseCase] and delegates add/remove/verify to their use cases, translating the
 * typed [DomainError] outcomes into one-shot notices.
 */
@HiltViewModel
class SourceFoldersViewModel
    @Inject
    constructor(
        observeSourceFolders: ObserveSourceFoldersUseCase,
        private val addSourceFolder: AddSourceFolderUseCase,
        private val removeSourceFolder: RemoveSourceFolderUseCase,
        private val verifySourceFoldersReady: VerifySourceFoldersReadyUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SourceFoldersUiState())
        val uiState: StateFlow<SourceFoldersUiState> = _uiState.asStateFlow()

        private val _events = Channel<SourceFoldersEvent>(Channel.BUFFERED)
        val events: Flow<SourceFoldersEvent> = _events.receiveAsFlow()

        init {
            viewModelScope.launch {
                observeSourceFolders().collect { folders ->
                    _uiState.update {
                        it.copy(
                            folders = folders.map { folder -> SourceFolderUi(folder.id, folder.displayPath) },
                            canContinue = folders.isNotEmpty(),
                        )
                    }
                }
            }
        }

        fun onCommand(command: SourceFoldersCommand) =
            when (command) {
                SourceFoldersCommand.AddFolderClicked -> emit(SourceFoldersEvent.LaunchFolderPicker)
                is SourceFoldersCommand.FolderPicked -> addFolder(command.treeUri)
                SourceFoldersCommand.SelectionCancelled -> emit(SourceFoldersEvent.NotifySelectionCancelled)
                is SourceFoldersCommand.RemoveFolder -> removeFolder(command.id)
                SourceFoldersCommand.ContinueClicked -> continueToScan()
            }

        private fun addFolder(treeUri: String) {
            viewModelScope.launch {
                val result = addSourceFolder(LibraryCommand.AddSourceFolder(treeUri))
                if (result is OperationResult.Failure) {
                    emit(result.error.toNotice())
                }
            }
        }

        private fun removeFolder(id: Long) {
            viewModelScope.launch {
                // A missing id fails silently: the list is the source of truth and re-emits on change.
                removeSourceFolder(LibraryCommand.RemoveSourceFolder(id))
            }
        }

        private fun continueToScan() {
            viewModelScope.launch {
                if (verifySourceFoldersReady()) {
                    emit(SourceFoldersEvent.NavigateToScan)
                }
            }
        }

        private fun DomainError.toNotice(): SourceFoldersEvent =
            when (this) {
                DomainError.DuplicateSourceFolder -> SourceFoldersEvent.NotifyDuplicate
                DomainError.PermissionDenied -> SourceFoldersEvent.NotifyPermissionDenied
                is DomainError.EntityNotFound -> SourceFoldersEvent.NotifyPermissionDenied
            }

        private fun emit(event: SourceFoldersEvent) {
            _events.trySend(event)
        }
    }
