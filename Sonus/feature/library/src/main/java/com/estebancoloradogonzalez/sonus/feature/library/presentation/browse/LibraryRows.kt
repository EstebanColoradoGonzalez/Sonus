package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView
import com.estebancoloradogonzalez.sonus.feature.library.R

/** Free-text search field of the library screen (US-011). A trailing clear button appears when the
 * field holds text. Called from `LibraryScreen`; the debounce lives in the ViewModel (contract §4.1). */
@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        placeholder = { Text(text = stringResource(R.string.library_search_hint)) },
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text(text = stringResource(R.string.library_search_clear))
                }
            }
        },
    )
}

/** Renders the current navigation level as a virtualized list (US-010). Called from `LibraryScreen`. */
@Composable
internal fun ContentList(
    content: LibraryContent,
    onCommand: (LibraryCommand) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        when (content) {
            is LibraryContent.Genres ->
                items(content.items, key = { it.id }) { genre ->
                    GenreRow(genre = genre, onClick = { onCommand(LibraryCommand.OpenGenre(genre)) })
                    HorizontalDivider()
                }
            is LibraryContent.Artists ->
                items(content.items, key = { it.id }) { artist ->
                    ArtistRow(artist = artist, onClick = { onCommand(LibraryCommand.OpenArtist(artist)) })
                    HorizontalDivider()
                }
            is LibraryContent.Albums ->
                items(content.items, key = { it.id }) { album ->
                    AlbumRow(album = album, onClick = { onCommand(LibraryCommand.OpenAlbum(album)) })
                    HorizontalDivider()
                }
            is LibraryContent.Tracks ->
                items(content.items, key = { it.id }) { track ->
                    TrackRow(track = track)
                    HorizontalDivider()
                }
        }
    }
}

@Composable
private fun GenreRow(
    genre: GenreView,
    onClick: () -> Unit,
) {
    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        headlineContent = { Text(text = genre.name.orLabel(R.string.library_no_genre)) },
        trailingContent = { Text(text = stringResource(R.string.library_item_count, genre.trackCount) + "  ›") },
    )
}

@Composable
private fun ArtistRow(
    artist: ArtistView,
    onClick: () -> Unit,
) {
    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        headlineContent = { Text(text = artist.name.orLabel(R.string.library_no_artist)) },
        trailingContent = { Text(text = stringResource(R.string.library_item_count, artist.trackCount) + "  ›") },
    )
}

@Composable
private fun AlbumRow(
    album: AlbumView,
    onClick: () -> Unit,
) {
    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        leadingContent = { ArtworkPlaceholder(name = album.name.orLabel(R.string.library_no_album)) },
        headlineContent = { Text(text = album.name.orLabel(R.string.library_no_album)) },
        supportingContent = { Text(text = album.artistName.orLabel(R.string.library_no_artist)) },
        trailingContent = { Text(text = stringResource(R.string.library_item_count, album.trackCount) + "  ›") },
    )
}

@Composable
private fun TrackRow(track: TrackView) {
    ListItem(
        headlineContent = { Text(text = track.title.orLabel(R.string.library_no_title)) },
        supportingContent = {
            Text(
                text =
                    track.artistName.orLabel(R.string.library_no_artist) + " · " +
                        track.albumName.orLabel(R.string.library_no_album),
            )
        },
        trailingContent = {
            if (track.availability == TrackAvailability.UNSUPPORTED) {
                Text(
                    text = stringResource(R.string.library_track_unsupported),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun ArtworkPlaceholder(name: String) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Resolves a blank/absent dimension name to its localized "Sin …" label (Invariant 4). */
@Composable
internal fun String?.orLabel(labelRes: Int): String = if (this.isNullOrBlank()) stringResource(labelRes) else this
