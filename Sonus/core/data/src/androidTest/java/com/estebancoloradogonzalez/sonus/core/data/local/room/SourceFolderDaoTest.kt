package com.estebancoloradogonzalez.sonus.core.data.local.room

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SourceFolderDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.SourceFolder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO test against an in-memory Room database (coding-standards §5: Room DAOs are
 * tested as instrumented tests). Covers insert/observe, the `treeUri` uniqueness (AC4) and deletion.
 */
@RunWith(AndroidJUnit4::class)
class SourceFolderDaoTest {
    private lateinit var database: SonusDatabase
    private lateinit var dao: SourceFolderDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SonusDatabase::class.java).build()
        dao = database.sourceFolderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsAndObservesFolder() =
        runTest {
            val id = dao.insert(SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L))

            val folders = dao.observeAll().first()

            assertThat(folders).hasSize(1)
            assertThat(folders.first().id).isEqualTo(id)
            assertThat(folders.first().displayPath).isEqualTo("Music")
        }

    @Test
    fun rejectsDuplicateTreeUri() =
        runTest {
            dao.insert(SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L))

            val exception =
                runCatching {
                    dao.insert(SourceFolder(treeUri = "content://tree/music", displayPath = "Music2", dateAddedMs = 2L))
                }.exceptionOrNull()

            assertThat(exception).isInstanceOf(SQLiteConstraintException::class.java)
            assertThat(dao.countByTreeUri("content://tree/music")).isEqualTo(1)
        }

    @Test
    fun findsByIdAndDeletes() =
        runTest {
            val id = dao.insert(SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L))

            assertThat(dao.findById(id)).isNotNull()

            dao.deleteById(id)

            assertThat(dao.findById(id)).isNull()
            assertThat(dao.observeAll().first()).isEmpty()
        }
}
