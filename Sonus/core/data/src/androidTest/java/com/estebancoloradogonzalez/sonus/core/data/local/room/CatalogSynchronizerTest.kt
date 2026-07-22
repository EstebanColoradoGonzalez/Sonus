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
 * resolution for absent metadata (AC9, Invariant 4), the orphan-dimension purge (§6.2), and the
 * write-side of the `INCREMENTAL` diff (US-008): a track discovered but not re-processed is preserved
 * while a track absent from the discovered set is purged (AC1/AC4), plus the [indexedFingerprints] map.
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
        fileLastModifiedMs: Long = 1L,
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
        fileLastModifiedMs = fileLastModifiedMs,
    )

    @Test
    fun resolvesAbsentMetadataToSentinel() =
        runTest {
            // Arrange (AC9) — a track with no artist/album/genre tags
            val track = scannedTrack("content://doc/a", null, null, null)
            val summary = synchronizer.sync(listOf(track), listOf(track.uri))

            // Assert — resolved to the id = 1 sentinels, never inferred
            assertThat(summary.added).isEqualTo(1)
            assertThat(database.trackDao().findIdByUri("content://doc/a")).isNotNull()
            assertThat(database.artistDao().findIdByName("")).isEqualTo(1)
        }

    @Test
    fun purgesOrphanDimensionsWhenTracksDisappear() =
        runTest {
            // Arrange — first scan creates the "Rock" artist through one track
            val track = scannedTrack("content://doc/a", "Rock", "Album", "Pop")
            synchronizer.sync(listOf(track), listOf(track.uri))
            assertThat(database.artistDao().findIdByName("Rock")).isNotNull()

            // Act — a scan with no files purges the track and its now-orphan dimensions (§6.2)
            val summary = synchronizer.sync(emptyList(), emptyList())

            // Assert
            assertThat(summary.purged).isEqualTo(1)
            assertThat(summary.orphanDimsPurged).isEqualTo(3)
            assertThat(database.artistDao().findIdByName("Rock")).isNull()
            assertThat(database.artistDao().findIdByName("")).isEqualTo(1)
        }

    @Test
    fun reScanOnNonEmptyCatalogAddsPurgesAndCountsSummary() =
        runTest {
            // Arrange — first scan populates a non-empty catalog with two distinct dimension sets
            val a = scannedTrack("content://doc/a", "Rock", "AlbumR", "Pop")
            val b = scannedTrack("content://doc/b", "Jazz", "AlbumJ", "Smooth")
            synchronizer.sync(listOf(a, b), listOf(a.uri, b.uri))

            // Act — a re-scan where: 'a' remains, 'b' disappears (purge), a new 'c' with absent
            // metadata is added, and an undecodable 'd' is indexed as UNSUPPORTED (AC3/4/6/7/8/9/10)
            val c = scannedTrack("content://doc/c", null, null, null)
            val d = scannedTrack("content://doc/d", null, null, null, TrackAvailability.UNSUPPORTED)
            val summary = synchronizer.sync(listOf(a, c, d), listOf(a.uri, c.uri, d.uri))

            // Assert — deterministic counters and coherent catalog
            assertThat(summary.added).isEqualTo(2)
            assertThat(summary.purged).isEqualTo(1)
            assertThat(summary.unsupported).isEqualTo(1)
            assertThat(summary.orphanDimsPurged).isEqualTo(3)
            assertThat(database.trackDao().findIdByUri("content://doc/b")).isNull()
            assertThat(database.trackDao().findIdByUri("content://doc/c")).isNotNull()
            assertThat(database.artistDao().findIdByName("Jazz")).isNull()
            assertThat(database.artistDao().findIdByName("Rock")).isNotNull()
            // Absent metadata resolved to the sentinel, never inferred (Invariant 4)
            assertThat(database.artistDao().findIdByName("")).isEqualTo(1)
        }

    @Test
    fun preservesSkippedTrackWhilePurgingMissingAndInsertingNew() =
        runTest {
            // Arrange — a non-empty catalog with 'a' and 'b'
            val a = scannedTrack("content://doc/a", "Rock", "AlbumR", "Pop")
            val b = scannedTrack("content://doc/b", "Jazz", "AlbumJ", "Smooth")
            synchronizer.sync(listOf(a, b), listOf(a.uri, b.uri))

            // Act — INCREMENTAL write-side: 'a' was skipped upstream (still discovered, NOT processed),
            // 'b' disappeared (absent from discovered), 'c' is new (AC1 preserve / AC4 purge)
            val c = scannedTrack("content://doc/c", "Blues", "AlbumC", "Soul")
            val summary = synchronizer.sync(listOf(c), listOf(a.uri, c.uri))

            // Assert — 'a' survives untouched, 'b' purged, 'c' inserted
            assertThat(summary.added).isEqualTo(1)
            assertThat(summary.purged).isEqualTo(1)
            assertThat(database.trackDao().findIdByUri("content://doc/a")).isNotNull()
            assertThat(database.trackDao().findIdByUri("content://doc/b")).isNull()
            assertThat(database.trackDao().findIdByUri("content://doc/c")).isNotNull()
            assertThat(database.artistDao().findIdByName("Rock")).isNotNull()
            assertThat(database.artistDao().findIdByName("Jazz")).isNull()
        }

    @Test
    fun indexedFingerprintsReturnsPersistedUriMtimeMap() =
        runTest {
            // Arrange — two tracks persisted with distinct on-disk modification times
            val a = scannedTrack("content://doc/a", "Rock", "AlbumR", "Pop", fileLastModifiedMs = 10L)
            val b = scannedTrack("content://doc/b", "Jazz", "AlbumJ", "Smooth", fileLastModifiedMs = 20L)
            synchronizer.sync(listOf(a, b), listOf(a.uri, b.uri))

            // Act
            val fingerprints = synchronizer.indexedFingerprints()

            // Assert — the uri → mtime map that drives the INCREMENTAL skip
            assertThat(fingerprints).containsExactly("content://doc/a", 10L, "content://doc/b", 20L)
        }

    private object FixedTimeProvider : TimeProvider {
        override fun nowMs(): Long = 1_000L
    }
}
