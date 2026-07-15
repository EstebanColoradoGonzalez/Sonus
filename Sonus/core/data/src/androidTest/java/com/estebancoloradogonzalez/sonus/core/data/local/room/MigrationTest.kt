package com.estebancoloradogonzalez.sonus.core.data.local.room

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.estebancoloradogonzalez.sonus.core.data.local.room.migration.MIGRATION_1_2
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates [MIGRATION_1_2] against the exported schema (`2.json`): it creates the catalog tables
 * with the expected structure and seeds the dimension sentinels (§6.1).
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

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
