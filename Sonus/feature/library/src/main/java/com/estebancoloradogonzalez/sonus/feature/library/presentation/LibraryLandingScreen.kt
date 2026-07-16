package com.estebancoloradogonzalez.sonus.feature.library.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.sonus.feature.library.R

/**
 * Transition target of the foundational scan (US-003). Placeholder for the real library view, which
 * belongs to later EPICs (EPIC-02+): US-003's scope ends at reaching this destination.
 *
 * Offers an additional access to Source Folders management (US-005 [onNavigateToSourceFolders]),
 * mirroring the primary entry in Settings.
 */
@Composable
fun LibraryLandingScreen(
    onNavigateToSourceFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.library_landing_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.library_landing_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onNavigateToSourceFolders,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(text = stringResource(R.string.library_add_source_folder))
        }
    }
}
