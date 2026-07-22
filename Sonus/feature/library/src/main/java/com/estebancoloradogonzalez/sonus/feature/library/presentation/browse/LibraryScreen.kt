package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.sonus.feature.library.R

/**
 * Library navigation screen (US-010, `TRG-NAV-01`). Presents the taxonomic dimensions as tabs
 * (Escenario 1), drills Genre → Artist → Album → Tracks (Escenario 4/5/6), renders sentinel groups
 * with localized labels (Escenario 8), flags `UNSUPPORTED` tracks as non-playable (Escenario 9) and
 * shows informative empty states without errors (Escenario 10/11). Playback is out of scope (US-013):
 * the screen only browses. The system back button navigates up the hierarchy before leaving. The list
 * rows and their label helpers live in `LibraryRows.kt`.
 */
@Composable
fun LibraryScreen(
    onNavigateToSourceFolders: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.canNavigateUp) {
        viewModel.onCommand(LibraryCommand.NavigateUp)
    }

    LibraryScaffold(
        uiState = uiState,
        modifier = modifier,
        onCommand = viewModel::onCommand,
        onNavigateToSourceFolders = onNavigateToSourceFolders,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScaffold(
    uiState: LibraryUiState,
    onCommand: (LibraryCommand) -> Unit,
    onNavigateToSourceFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.library_title)) },
                actions = {
                    TextButton(onClick = onNavigateToSourceFolders) {
                        Text(text = stringResource(R.string.library_manage_folders))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            DimensionTabs(
                selected = uiState.selectedDimension,
                onSelect = { onCommand(LibraryCommand.SelectDimension(it)) },
            )
            if (uiState.canNavigateUp) {
                Breadcrumb(
                    node = uiState.currentNode,
                    onNavigateUp = { onCommand(LibraryCommand.NavigateUp) },
                )
            }
            LibraryBody(uiState = uiState, onCommand = onCommand)
        }
    }
}

@Composable
private fun DimensionTabs(
    selected: BrowseDimension,
    onSelect: (BrowseDimension) -> Unit,
) {
    val selectedIndex = BrowseDimension.entries.indexOf(selected)
    ScrollableTabRow(selectedTabIndex = selectedIndex) {
        BrowseDimension.entries.forEach { dimension ->
            Tab(
                selected = dimension == selected,
                onClick = { onSelect(dimension) },
                text = { Text(text = stringResource(dimension.labelRes())) },
            )
        }
    }
}

@Composable
private fun Breadcrumb(
    node: LibraryNode,
    onNavigateUp: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onNavigateUp) {
            Text(text = "‹ " + stringResource(R.string.library_back))
        }
        Text(
            text = node.breadcrumbTitle(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun LibraryBody(
    uiState: LibraryUiState,
    onCommand: (LibraryCommand) -> Unit,
) {
    when {
        uiState.isLoading -> LoadingBody()
        uiState.content.isEmpty -> EmptyBody(uiState = uiState)
        else -> ContentList(content = uiState.content, onCommand = onCommand)
    }
}

@Composable
private fun LoadingBody() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyBody(uiState: LibraryUiState) {
    val message =
        if (uiState.isCatalogEmpty) {
            stringResource(R.string.library_empty_catalog)
        } else {
            stringResource(uiState.content.emptyMessageRes())
        }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LibraryNode.breadcrumbTitle(): String =
    when (this) {
        is LibraryNode.Root -> stringResource(dimension.labelRes())
        is LibraryNode.GenreArtists -> genreName.orLabel(R.string.library_no_genre)
        is LibraryNode.ArtistAlbums -> artistName.orLabel(R.string.library_no_artist)
        is LibraryNode.AlbumTracks -> albumName.orLabel(R.string.library_no_album)
    }

private fun BrowseDimension.labelRes(): Int =
    when (this) {
        BrowseDimension.MUSIC -> R.string.library_tab_music
        BrowseDimension.PODCASTS -> R.string.library_tab_podcasts
        BrowseDimension.GENRES -> R.string.library_tab_genres
        BrowseDimension.ARTISTS -> R.string.library_tab_artists
        BrowseDimension.ALBUMS -> R.string.library_tab_albums
    }

private fun LibraryContent.emptyMessageRes(): Int =
    when (this) {
        is LibraryContent.Genres -> R.string.library_empty_genres
        is LibraryContent.Artists -> R.string.library_empty_artists
        is LibraryContent.Albums -> R.string.library_empty_albums
        is LibraryContent.Tracks -> R.string.library_empty_tracks
    }
