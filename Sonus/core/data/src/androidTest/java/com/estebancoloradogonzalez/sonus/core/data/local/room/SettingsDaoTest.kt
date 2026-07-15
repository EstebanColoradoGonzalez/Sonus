package com.estebancoloradogonzalez.sonus.core.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SettingsDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.migration.APP_SETTINGS_SINGLETON_SEED
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO test against an in-memory Room database (coding-standards §5). The singleton is
 * seeded with the shared [APP_SETTINGS_SINGLETON_SEED] (mirroring fresh installs). Covers the
 * `false` default (Escenario 5), the mark (Escenario 1) and its idempotency (Escenario 2).
 */
@RunWith(AndroidJUnit4::class)
class SettingsDaoTest {
    private lateinit var database: SonusDatabase
    private lateinit var dao: SettingsDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, SonusDatabase::class.java)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            APP_SETTINGS_SINGLETON_SEED.forEach(db::execSQL)
                        }
                    },
                ).build()
        dao = database.settingsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun defaultsToNotCompleted() =
        runTest {
            assertThat(dao.isOnboardingCompleted()).isFalse()
        }

    @Test
    fun marksOnboardingCompleted() =
        runTest {
            dao.markOnboardingCompleted()

            assertThat(dao.isOnboardingCompleted()).isTrue()
        }

    @Test
    fun markingIsIdempotent() =
        runTest {
            dao.markOnboardingCompleted()
            dao.markOnboardingCompleted()

            assertThat(dao.isOnboardingCompleted()).isTrue()
        }
}
