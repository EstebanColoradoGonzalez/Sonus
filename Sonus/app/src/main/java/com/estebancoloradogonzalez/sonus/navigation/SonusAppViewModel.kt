package com.estebancoloradogonzalez.sonus.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.sonus.core.domain.command.SettingsCommand
import com.estebancoloradogonzalez.sonus.core.domain.usecase.CompleteOnboardingUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.IsOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level coordinator of the first-run gating (US-004). On creation it resolves the start
 * destination by reading `AppSettings.onboardingCompleted` (Escenario 3/5) and exposes
 * [completeOnboarding] to close the flow on the SCAN → LIBRARY transition (`TRG-CFG-02`,
 * Escenario 1/4). Holds no business rule nor I/O; delegates to the use cases.
 */
@HiltViewModel
class SonusAppViewModel
    @Inject
    constructor(
        private val isOnboardingCompleted: IsOnboardingCompletedUseCase,
        private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    ) : ViewModel() {
        private val _startDestination = MutableStateFlow<StartDestinationUiState>(StartDestinationUiState.Loading)
        val startDestination: StateFlow<StartDestinationUiState> = _startDestination.asStateFlow()

        init {
            viewModelScope.launch {
                _startDestination.value =
                    if (isOnboardingCompleted()) {
                        StartDestinationUiState.Library
                    } else {
                        StartDestinationUiState.Onboarding
                    }
            }
        }

        /**
         * Marks the onboarding as completed on the transition to the library. Fire-and-forget: the
         * result is intentionally ignored so a persistence failure degrades gracefully without
         * blocking the transition (Escenario 6); the operation is idempotent and retriable.
         */
        fun completeOnboarding() {
            viewModelScope.launch { completeOnboardingUseCase(SettingsCommand.CompleteOnboarding) }
        }
    }
