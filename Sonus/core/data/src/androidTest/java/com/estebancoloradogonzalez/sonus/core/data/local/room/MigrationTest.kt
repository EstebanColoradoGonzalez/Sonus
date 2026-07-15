package com.estebancoloradogonzalez.sonus.core.data.local.room

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.sonus.core.data.local.room.migration.MIGRATION_1_2
import com.estebancoloradogonzalez.sonus.core.data.local.room.migration.MIGRATION_2_3
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the versioned migrations against the exported schemas: [MIGRATION_1_2] creates the
 * catalog tables and seeds the dimension sentinels (§6.1); [MIGRATION_2_3] adds the `app_settings`
 * singleton and seeds it with its Big Bang defaults (§6.1).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SonusDatabase::class.java,
        )

    @Test
    fun migrate1To2CreatesCatalogSchemaAndSeedsSentinels() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT name FROM artist WHERE id = 1").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEmpty()
        }
        db.query("SELECT artistId FROM album WHERE id = 1").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(1L)
        }
        db.close()
    }

    @Test
    fun migrate2To3AddsAppSettingsAndSeedsSingleton() {
        helper.createDatabase(TEST_DB, 2).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.query(
            "SELECT onboardingCompleted, themePreference FROM app_settings WHERE id = 1",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(0)
            assertThat(cursor.getString(1)).isEqualTo("SYSTEM")
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
