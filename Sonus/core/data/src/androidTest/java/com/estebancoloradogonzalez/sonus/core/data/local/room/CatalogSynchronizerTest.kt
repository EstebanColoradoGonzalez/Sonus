package com.estebancoloradogonzalez.sonus.core.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Album
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Artist
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Genre
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.ScannedTrack
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.estebancoloradogonzalez.sonus.core.domain.port.TimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test of [CatalogSynchronizer] against an in-memory Room database. Covers the sentinel
 * resolution for absent metadata (AC3, Invariant 4) and the orphan-dimension purge (§6.2).
 */
@RunWith(AndroidJUnit4::class)
class CatalogSynchronizerTest {
    private lateinit var database: SonusDatabase
    private lateinit var synchronizer: CatalogSynchronizer
    private var sourceFolderId: Long = 0

    @Before
    fun setUp() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database = Room.inMemoryDatabaseBuilder(context, SonusDatabase::class.java).build()
            // Seed the dimension sentinels the migration/callback would create (§6.1).
            database.artistDao().insertIgnore(Artist(id = 1, name = ""))
            database.genreDao().insertIgnore(Genre(id = 1, name = ""))
            database.albumDao().insertIgnore(Album(id = 1, name = "", artistId = 1))
            sourceFolderId =
                database.sourceFolderDao().insert(
                    SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L),
                )
            synchronizer =
                CatalogSynchronizer(
                    database = database,
                    artistDao = database.artistDao(),
                    genreDao = database.genreDao(),
                    albumDao = database.albumDao(),
                    trackDao = database.trackDao(),
                    timeProvider = FixedTimeProvider,
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    private fun scannedTrack(
        uri: String,
        artistName: String?,
        albumName: String?,
        genreName: String?,
        availability: TrackAvailability = TrackAvailability.AVAILABLE,
    ) = ScannedTrack(
        uri = uri,
        title = null,
        artistName = artistName,
        albumName = albumName,
        genreName = genreName,
        contentType = ContentType.UNKNOWN,
        trackNumber = null,
        releaseYear = null,
        durationMs = 1_000L,
        hasEmbeddedArtwork = false,
        availability = availability,
        sourceFolderId = sourceFolderId,
        fileLastModifiedMs = 1L,
    )

    @Test
    fun resolvesAbsentMetadataToSentinel() =
        runTest {
            // Arrange (AC3) — a track with no artist/album/genre tags
            val summary = synchronizer.sync(listOf(scannedTrack("content://doc/a", null, null, null)))

            // Assert — resolved to the id = 1 sentinels, never inferred
            assertThat(summary.added).isEqualTo(1)
            assertThat(database.trackDao().findIdByUri("content://doc/a")).isNotNull()
            assertThat(database.artistDao().findIdByName("")).isEqualTo(1)
        }

    @Test
    fun purgesOrphanDimensionsWhenTracksDisappear() =
        runTest {
            // Arrange — first scan creates the "Rock" artist through one track
            synchronizer.sync(listOf(scannedTrack("content://doc/a", "Rock", "Album", "Pop")))
            assertThat(database.artistDao().findIdByName("Rock")).isNotNull()

            // Act — a scan with no files purges the track and its now-orphan dimensions (§6.2)
            val summary = synchronizer.sync(emptyList())

            // Assert
            assertThat(summary.purged).isEqualTo(1)
            assertThat(summary.orphanDimsPurged).isEqualTo(3)
            assertThat(database.artistDao().findIdByName("Rock")).isNull()
            assertThat(database.artistDao().findIdByName("")).isEqualTo(1)
        }

    private object FixedTimeProvider : TimeProvider {
        override fun nowMs(): Long = 1_000L
    }
}
