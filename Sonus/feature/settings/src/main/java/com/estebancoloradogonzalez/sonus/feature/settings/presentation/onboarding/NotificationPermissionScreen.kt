package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.sonus.feature.settings.R
import kotlinx.coroutines.launch

/**
 * Full-screen rationale of the first-run flow (US-001). Explains why notifications are useful, then
 * launches the OS `POST_NOTIFICATIONS` dialog. Owns the [ActivityResultContracts.RequestPermission]
 * launcher and computes permanent-denial (Scenario 4) at the edge via
 * [ActivityCompat.shouldShowRequestPermissionRationale]; the flow decisions belong to the ViewModel.
 */
@Composable
fun NotificationPermissionScreen(
    onNavigateToSourceFolders: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val degradedNotice = stringResource(R.string.onboarding_notifications_denied_notice)

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            val permanentlyDenied = !granted && !context.canRequestNotificationPermission()
            viewModel.onCommand(OnboardingCommand.PermissionResult(granted, permanentlyDenied))
        }

    LaunchedEffect(Unit) { viewModel.onCommand(OnboardingCommand.EvaluateStep) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.LaunchSystemPermissionDialog ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                OnboardingEvent.NavigateToSourceFolders -> onNavigateToSourceFolders()
                OnboardingEvent.OpenNotificationSettings -> context.openNotificationSettings()
                OnboardingEvent.NotifyNotificationsDegraded ->
                    scope.launch { snackbarHostState.showSnackbar(degradedNotice) }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState.phase) {
            OnboardingPermissionPhase.Rationale ->
                RationaleContent(
                    padding = innerPadding,
                    onAllow = { viewModel.onCommand(OnboardingCommand.RequestPermission) },
                    onSkip = { viewModel.onCommand(OnboardingCommand.Skip) },
                )
            OnboardingPermissionPhase.PermanentlyDenied ->
                PermanentlyDeniedContent(
                    padding = innerPadding,
                    onOpenSettings = { viewModel.onCommand(OnboardingCommand.OpenSettings) },
                    onContinue = { viewModel.onCommand(OnboardingCommand.Skip) },
                )
        }
    }
}

@Composable
private fun RationaleContent(
    padding: PaddingValues,
    onAllow: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_notifications_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_notifications_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(
            onClick = onAllow,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
        ) {
            Text(text = stringResource(R.string.onboarding_notifications_allow))
        }
        TextButton(
            onClick = onSkip,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
        ) {
            Text(text = stringResource(R.string.onboarding_notifications_skip))
        }
    }
}

@Composable
private fun PermanentlyDeniedContent(
    padding: PaddingValues,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_notifications_blocked_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_notifications_blocked_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(
            onClick = onOpenSettings,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
        ) {
            Text(text = stringResource(R.string.onboarding_notifications_open_settings))
        }
        TextButton(
            onClick = onContinue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
        ) {
            Text(text = stringResource(R.string.onboarding_continue))
        }
    }
}

private fun Context.canRequestNotificationPermission(): Boolean {
    val activity = findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.POST_NOTIFICATIONS,
    )
}

private fun Context.openNotificationSettings() {
    val appNotificationSettings =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    val intent =
        if (appNotificationSettings.resolveActivity(packageManager) != null) {
            appNotificationSettings
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
        }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
