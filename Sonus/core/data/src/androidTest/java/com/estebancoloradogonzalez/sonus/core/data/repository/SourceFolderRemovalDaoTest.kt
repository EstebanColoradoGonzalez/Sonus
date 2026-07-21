package com.estebancoloradogonzalez.sonus.core.data.repository

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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test of [SourceFolderRepositoryImpl.remove] against an in-memory Room database
 * (coding-standards §5). Covers the US-006 cascade: removing a folder purges its tracks
 * (`onDelete = CASCADE`) and the dimensions left orphan (§6.2), preserving the `id = 1` sentinels and
 * the tracks/dimensions of other folders (Scenarios 2/4).
 */
@RunWith(AndroidJUnit4::class)
class SourceFolderRemovalDaoTest {
    private lateinit var database: SonusDatabase
    private lateinit var repository: SourceFolderRepositoryImpl

    @Before
    fun setUp() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database = Room.inMemoryDatabaseBuilder(context, SonusDatabase::class.java).build()
            repository =
                SourceFolderRepositoryImpl(
                    database = database,
                    dao = database.sourceFolderDao(),
                    trackDao = database.trackDao(),
                    artistDao = database.artistDao(),
                    albumDao = database.albumDao(),
                    genreDao = database.genreDao(),
                )
            // Seed the dimension sentinels (id = 1), normally seeded by the creation callback.
            database.artistDao().insertIgnore(Artist(id = 1, name = ""))
            database.genreDao().insertIgnore(Genre(id = 1, name = ""))
            database.albumDao().insertIgnore(Album(id = 1, name = "", artistId = 1))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun removesFolderCascadingTracksAndPurgingOrphanDimensions() =
        runTest {
            // Arrange: two folders, each with its own dimensions and tracks.
            val folderA = database.sourceFolderDao().insert(SourceFolder(treeUri = "content://tree/a", displayPath = "A", dateAddedMs = 1L))
            val folderB = database.sourceFolderDao().insert(SourceFolder(treeUri = "content://tree/b", displayPath = "B", dateAddedMs = 2L))

            database.artistDao().insertIgnore(Artist(name = "Rock Artist"))
            val rockArtist = database.artistDao().findIdByName("Rock Artist")!!
            database.genreDao().insertIgnore(Genre(name = "Rock"))
            val rockGenre = database.genreDao().findIdByName("Rock")!!
            database.albumDao().insertIgnore(Album(name = "Rock Album", artistId = rockArtist))
            val rockAlbum = database.albumDao().findId("Rock Album", rockArtist)!!

            database.artistDao().insertIgnore(Artist(name = "Jazz Artist"))
            val jazzArtist = database.artistDao().findIdByName("Jazz Artist")!!
            database.genreDao().insertIgnore(Genre(name = "Jazz"))
            val jazzGenre = database.genreDao().findIdByName("Jazz")!!
            database.albumDao().insertIgnore(Album(name = "Jazz Album", artistId = jazzArtist))
            val jazzAlbum = database.albumDao().findId("Jazz Album", jazzArtist)!!

            database.trackDao().insert(track("content://a/1", rockArtist, rockAlbum, rockGenre, folderA))
            database.trackDao().insert(track("content://a/2", rockArtist, rockAlbum, rockGenre, folderA))
            database.trackDao().insert(track("content://b/1", jazzArtist, jazzAlbum, jazzGenre, folderB))

            assertThat(repository.countTracksUnder(folderA)).isEqualTo(2)

            // Act
            repository.remove(folderA)

            // Assert: folder A and its tracks gone; folder B intact.
            assertThat(database.sourceFolderDao().findById(folderA)).isNull()
            assertThat(repository.countTracksUnder(folderA)).isEqualTo(0)
            assertThat(repository.countTracksUnder(folderB)).isEqualTo(1)

            // Orphan dimensions purged; the still-referenced ones and the sentinels preserved.
            assertThat(database.artistDao().findIdByName("Rock Artist")).isNull()
            assertThat(database.genreDao().findIdByName("Rock")).isNull()
            assertThat(database.albumDao().findId("Rock Album", rockArtist)).isNull()
            assertThat(database.artistDao().findIdByName("Jazz Artist")).isEqualTo(jazzArtist)
            assertThat(database.genreDao().findIdByName("Jazz")).isEqualTo(jazzGenre)
            assertThat(database.albumDao().findId("Jazz Album", jazzArtist)).isEqualTo(jazzAlbum)
            assertThat(database.artistDao().findIdByName("")).isEqualTo(1L)
            assertThat(database.genreDao().findIdByName("")).isEqualTo(1L)
        }

    private fun track(
        uri: String,
        artistId: Long,
        albumId: Long,
        genreId: Long,
        sourceFolderId: Long,
    ) = Track(
        uri = uri,
        title = "Title",
        artistId = artistId,
        albumId = albumId,
        genreId = genreId,
        sourceFolderId = sourceFolderId,
        contentType = ContentType.MUSIC,
        trackNumber = null,
        releaseYear = null,
        durationMs = 1000L,
        hasEmbeddedArtwork = false,
        availability = TrackAvailability.AVAILABLE,
        fileLastModifiedMs = 1L,
        dateAddedMs = 1L,
    )
}
