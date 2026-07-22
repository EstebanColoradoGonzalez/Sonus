package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estebancoloradogonzalez.sonus.core.data.local.room.SonusDatabase
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Album
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Artist
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Genre
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.SourceFolder
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Track
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test of [CatalogBrowseDao] against an in-memory Room database (coding-standards §5:
 * DAO tests are instrumented). Covers the US-010 acceptance criteria that live in SQL: content-type
 * filtering (Esc 2/3), genre → artist listing (Esc 4), artist → album listing by `Album.artistId`
 * (Esc 5), album → track ordering by track number (Esc 6), sentinel grouping (Esc 8), `UNSUPPORTED`
 * visible / `MISSING` excluded (Esc 9, Invariant 2), the empty catalog signal (Esc 10) and the
 * `hasArtwork` derivation ([F-5]).
 */
@RunWith(AndroidJUnit4::class)
class CatalogBrowseDaoTest {
    private lateinit var database: SonusDatabase
    private lateinit var dao: CatalogBrowseDao
    private var sourceFolderId: Long = 0

    @Before
    fun setUp() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database = Room.inMemoryDatabaseBuilder(context, SonusDatabase::class.java).build()
            dao = database.catalogBrowseDao()
            // Sentinels the migration/callback would seed (§6.1).
            database.artistDao().insertIgnore(Artist(id = 1, name = ""))
            database.genreDao().insertIgnore(Genre(id = 1, name = ""))
            database.albumDao().insertIgnore(Album(id = 1, name = "", artistId = 1))
            sourceFolderId =
                database.sourceFolderDao().insert(
                    SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedCatalog() {
        database.genreDao().insertIgnore(Genre(id = 10, name = "Rock"))
        database.genreDao().insertIgnore(Genre(id = 11, name = "Jazz"))
        database.artistDao().insertIgnore(Artist(id = 20, name = "Queen"))
        database.artistDao().insertIgnore(Artist(id = 21, name = "Miles"))
        database.albumDao().insertIgnore(Album(id = 30, name = "Opera", artistId = 20))
        database.albumDao().insertIgnore(Album(id = 31, name = "Kind", artistId = 21))
        insertTrack("t1", "B", 20, 30, 10, ContentType.MUSIC, TrackAvailability.AVAILABLE, 2, true)
        insertTrack("t2", "A", 20, 30, 10, ContentType.MUSIC, TrackAvailability.AVAILABLE, 1, false)
        insertTrack("t3", "Pod", 21, 31, 11, ContentType.PODCAST, TrackAvailability.AVAILABLE, 1, false)
        insertTrack("t4", "U", 20, 30, 10, ContentType.MUSIC, TrackAvailability.UNSUPPORTED, null, false)
        insertTrack("t5", "Gone", 21, 31, 11, ContentType.MUSIC, TrackAvailability.MISSING, 3, false)
        insertTrack("t6", null, 1, 1, 1, ContentType.MUSIC, TrackAvailability.AVAILABLE, null, false)
    }

    @Suppress("LongParameterList")
    private suspend fun insertTrack(
        uri: String,
        title: String?,
        artistId: Long,
        albumId: Long,
        genreId: Long,
        contentType: ContentType,
        availability: TrackAvailability,
        trackNumber: Int?,
        hasEmbeddedArtwork: Boolean,
    ) {
        database.trackDao().insert(
            Track(
                uri = uri,
                title = title,
                artistId = artistId,
                albumId = albumId,
                genreId = genreId,
                sourceFolderId = sourceFolderId,
                contentType = contentType,
                trackNumber = trackNumber,
                releaseYear = null,
                durationMs = 1_000L,
                hasEmbeddedArtwork = hasEmbeddedArtwork,
                availability = availability,
                fileLastModifiedMs = 1L,
                dateAddedMs = 1L,
            ),
        )
    }

    @Test
    fun browseTracksByMusicExcludesPodcastsAndMissing() =
        runTest {
            // Arrange (Esc 2 / Invariant 2)
            seedCatalog()

            // Act
            val result = dao.browseTracks(ContentType.MUSIC, null, null, null, null).first()

            // Assert — only MUSIC and non-MISSING tracks (t1, t2, t4, t6)
            assertThat(result.map { it.uri }).containsExactly("t1", "t2", "t4", "t6")
            assertThat(result.none { it.availability == TrackAvailability.MISSING }).isTrue()
        }

    @Test
    fun browseTracksByPodcastReturnsOnlyPodcasts() =
        runTest {
            // Arrange (Esc 3)
            seedCatalog()

            // Act
            val result = dao.browseTracks(ContentType.PODCAST, null, null, null, null).first()

            // Assert
            assertThat(result.map { it.uri }).containsExactly("t3")
        }

    @Test
    fun genresListVisibleGenresWithSentinelLast() =
        runTest {
            // Arrange (Esc 4 / Esc 8)
            seedCatalog()

            // Act
            val result = dao.genres(null).first()

            // Assert — Rock(3), Jazz(1), sentinel(1); the blank-name sentinel sorts last
            assertThat(result.map { it.name }).containsExactly("Rock", "Jazz", "").inOrder()
            val rock = result.first { it.id == 10L }
            assertThat(rock.trackCount).isEqualTo(3)
            assertThat(result.last().id).isEqualTo(1L)
        }

    @Test
    fun artistsWithinGenreListsOnlyMatchingArtists() =
        runTest {
            // Arrange (Esc 4)
            seedCatalog()

            // Act
            val result = dao.artists(genreId = 10, contentType = null).first()

            // Assert — only Queen has Rock tracks, counting the 3 visible ones
            assertThat(result.map { it.id }).containsExactly(20L)
            assertThat(result.first().trackCount).isEqualTo(3)
        }

    @Test
    fun albumsByArtistUseAlbumArtistIdAndDeriveArtwork() =
        runTest {
            // Arrange (Esc 5 / [F-7] / [F-5])
            seedCatalog()

            // Act
            val withArtwork = dao.albums(artistId = 20, contentType = null).first()
            val withoutArtwork = dao.albums(artistId = 21, contentType = null).first()

            // Assert — Opera belongs to Queen (Album.artistId=20), has embedded artwork via t1
            assertThat(withArtwork.map { it.id }).containsExactly(30L)
            assertThat(withArtwork.first().hasArtwork).isTrue()
            assertThat(withArtwork.first().trackCount).isEqualTo(3)
            // Kind belongs to Miles; its only visible track (t3) has no artwork
            assertThat(withoutArtwork.first().hasArtwork).isFalse()
        }

    @Test
    fun albumTracksOrderedByTrackNumberNullsLastKeepUnsupported() =
        runTest {
            // Arrange (Esc 6 / Esc 9)
            seedCatalog()

            // Act
            val result = dao.browseAlbumTracks(albumId = 30, availability = null, textFilter = null).first()

            // Assert — t2 (#1), t1 (#2), t4 (no number → last); UNSUPPORTED stays visible
            assertThat(result.map { it.uri }).containsExactly("t2", "t1", "t4").inOrder()
            assertThat(result.last().availability).isEqualTo(TrackAvailability.UNSUPPORTED)
        }

    @Test
    fun browseTracksByTitleTextFilterMatchesOnlyTitle() =
        runTest {
            // Arrange (US-011 Esc 1) — "Pod" only appears in t3's title
            seedCatalog()

            // Act
            val result = dao.browseTracks(null, null, null, null, "Pod").first()

            // Assert
            assertThat(result.map { it.uri }).containsExactly("t3")
        }

    @Test
    fun browseTracksByArtistTextFilterIsCaseInsensitiveAndExcludesSentinel() =
        runTest {
            // Arrange (US-011 Esc 1) — lowercase "queen" matches the "Queen" artist of t1/t2/t4
            seedCatalog()

            // Act
            val result = dao.browseTracks(ContentType.MUSIC, null, null, null, "queen").first()

            // Assert — case-insensitive artist match; the blank-name sentinel track (t6) never matches
            assertThat(result.map { it.uri }).containsExactly("t1", "t2", "t4")
            assertThat(result.map { it.uri }).doesNotContain("t6")
        }

    @Test
    fun browseTracksByAlbumTextFilterIsCaseInsensitive() =
        runTest {
            // Arrange (US-011 Esc 1) — lowercase "opera" matches the "Opera" album of t1/t2/t4
            seedCatalog()

            // Act
            val result = dao.browseTracks(null, null, null, null, "opera").first()

            // Assert
            assertThat(result.map { it.uri }).containsExactly("t1", "t2", "t4")
        }

    @Test
    fun browseTracksTextFilterWithoutMatchesReturnsEmpty() =
        runTest {
            // Arrange (US-011 Esc 2) — no track matches the term
            seedCatalog()

            // Act
            val result = dao.browseTracks(null, null, null, null, "zzz").first()

            // Assert — empty list, never an error
            assertThat(result).isEmpty()
        }

    @Test
    fun browseTracksTextFilterIntersectsWithContentType() =
        runTest {
            // Arrange (US-011 Esc 4) — "Pod" alone matches t3, but t3 is a PODCAST
            seedCatalog()

            // Act
            val result = dao.browseTracks(ContentType.MUSIC, null, null, null, "Pod").first()

            // Assert — the MUSIC dimension intersected with the text yields nothing
            assertThat(result).isEmpty()
        }

    @Test
    fun browseAlbumTracksAppliesTextFilter() =
        runTest {
            // Arrange (US-011 Esc 4) — within album Opera only t4's title contains "U"
            seedCatalog()

            // Act
            val result = dao.browseAlbumTracks(albumId = 30, availability = null, textFilter = "U").first()

            // Assert
            assertThat(result.map { it.uri }).containsExactly("t4")
        }

    @Test
    fun observeCatalogEmptyReflectsVisibleTrackPresence() =
        runTest {
            // Arrange (Esc 10) — no tracks yet
            assertThat(dao.observeCatalogEmpty().first()).isTrue()
            assertThat(dao.genres(null).first()).isEmpty()

            // Act — add a catalog
            seedCatalog()

            // Assert — no longer empty
            assertThat(dao.observeCatalogEmpty().first()).isFalse()
        }
}
