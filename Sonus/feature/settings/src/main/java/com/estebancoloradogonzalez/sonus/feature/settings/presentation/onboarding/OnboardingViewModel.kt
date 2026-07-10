package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

import androidx.lifecycle.ViewModel
import com.estebancoloradogonzalez.sonus.core.domain.model.NotificationPermissionStep
import com.estebancoloradogonzalez.sonus.core.domain.usecase.EvaluateNotificationPermissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Drives the notification-permission step of the first-run flow (US-001).
 *
 * Holds no business rule nor I/O: it delegates the skip/request decision to
 * [EvaluateNotificationPermissionUseCase] and translates the OS dialog outcome, reported by the
 * screen through [OnboardingCommand.PermissionResult], into state and one-shot events.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val evaluateNotificationPermission: EvaluateNotificationPermissionUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
        val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

        fun onCommand(command: OnboardingCommand) =
            when (command) {
                OnboardingCommand.EvaluateStep -> evaluateStep()
                OnboardingCommand.RequestPermission -> emit(OnboardingEvent.LaunchSystemPermissionDialog)
                OnboardingCommand.Skip -> navigateToSourceFolders()
                is OnboardingCommand.PermissionResult -> handlePermissionResult(command)
                OnboardingCommand.OpenSettings -> emit(OnboardingEvent.OpenNotificationSettings)
            }

        private fun evaluateStep() {
            when (evaluateNotificationPermission()) {
                NotificationPermissionStep.Skip -> navigateToSourceFolders()
                NotificationPermissionStep.Request ->
                    _uiState.update { it.copy(phase = OnboardingPermissionPhase.Rationale) }
            }
        }

        private fun handlePermissionResult(result: OnboardingCommand.PermissionResult) {
            when {
                result.granted -> navigateToSourceFolders()
                result.permanentlyDenied ->
                    _uiState.update { it.copy(phase = OnboardingPermissionPhase.PermanentlyDenied) }
                else -> {
                    emit(OnboardingEvent.NotifyNotificationsDegraded)
                    navigateToSourceFolders()
                }
            }
        }

        private fun navigateToSourceFolders() = emit(OnboardingEvent.NavigateToSourceFolders)

        private fun emit(event: OnboardingEvent) {
            _events.trySend(event)
        }
    }
