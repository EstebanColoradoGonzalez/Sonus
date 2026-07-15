package com.estebancoloradogonzalez.sonus.core.data.repository

import com.estebancoloradogonzalez.sonus.core.data.fake.FakeId3DataSource
import com.estebancoloradogonzalez.sonus.core.data.fake.FakeSafDataSource
import com.estebancoloradogonzalez.sonus.core.data.fake.RecordingScanStateEmitter
import com.estebancoloradogonzalez.sonus.core.data.fake.TestDispatcherProvider
import com.estebancoloradogonzalez.sonus.core.data.id3.RawTrackMetadata
import com.estebancoloradogonzalez.sonus.core.data.local.room.CatalogSynchronizer
import com.estebancoloradogonzalez.sonus.core.data.local.saf.DiscoveredFile
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.model.ScannedTrack
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogRepositoryImplTest {
    private val synchronizer = mockk<CatalogSynchronizer>()

    private fun folder(
        id: Long,
        treeUri: String,
    ) = SourceFolder(id = id, treeUri = treeUri, displayPath = "F$id", dateAddedMs = id)

    private fun available(uri: String) =
        RawTrackMetadata(
            title = uri,
            artistName = "Artist",
            albumName = "Album",
            genreName = "Genre",
            contentType = ContentType.UNKNOWN,
            trackNumber = 1,
            releaseYear = 2020,
            durationMs = 1_000L,
            hasEmbeddedArtwork = false,
            availability = TrackAvailability.AVAILABLE,
        )

    private fun unsupported(uri: String) = available(uri).copy(availability = TrackAvailability.UNSUPPORTED)

    @Test
    fun `scans every file, keeps unsupported ones and reports deterministic progress`() =
        runTest {
            // Arrange (AC1/AC4) — one available, one unsupported file under a single folder
            val treeUri = "content://tree/music"
            val fileA = DiscoveredFile("content://doc/a", "audio/mpeg", 10L)
            val fileB = DiscoveredFile("content://doc/b", "audio/x-weird", 20L)
            val saf = FakeSafDataSource(mapOf(treeUri to listOf(fileA, fileB)))
            val id3 =
                FakeId3DataSource(
                    mapOf(fileA.uri to available(fileA.uri), fileB.uri to unsupported(fileB.uri)),
                )
            val emitter = RecordingScanStateEmitter()
            val summary = ScanSummary(added = 2, purged = 0, unsupported = 1, orphanDimsPurged = 0)
            val scannedSlot = slot<List<ScannedTrack>>()
            coEvery { synchronizer.sync(capture(scannedSlot)) } returns summary
            val repository =
                CatalogRepositoryImpl(
                    safDataSource = saf,
                    id3DataSource = id3,
                    catalogSynchronizer = synchronizer,
                    scanStateEmitter = emitter,
                    dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler)),
                )

            // Act
            val result = repository.synchronize(listOf(folder(1L, treeUri)), ScanMode.FULL)

            // Assert
            assertThat(result).isEqualTo(OperationResult.Success(summary))
            assertThat(scannedSlot.captured).hasSize(2)
            assertThat(scannedSlot.captured.map { it.availability })
                .containsExactly(TrackAvailability.AVAILABLE, TrackAvailability.UNSUPPORTED)
            assertThat(emitter.emissions.first()).isEqualTo(ScanState.Scanning(processed = 0, total = null))
            assertThat(emitter.emissions).contains(ScanState.Scanning(processed = 2, total = 2))
            assertThat(emitter.emissions.last()).isEqualTo(ScanState.Syncing)
        }

    @Test
    fun `returns ScanAborted when a folder loses SAF access`() =
        runTest {
            // Arrange (AC5)
            val saf = FakeSafDataSource(throwSecurity = true)
            val emitter = RecordingScanStateEmitter()
            val repository =
                CatalogRepositoryImpl(
                    safDataSource = saf,
                    id3DataSource = FakeId3DataSource(),
                    catalogSynchronizer = synchronizer,
                    scanStateEmitter = emitter,
                    dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler)),
                )

            // Act
            val result = repository.synchronize(listOf(folder(1L, "content://tree/x")), ScanMode.FULL)

            // Assert
            assertThat(result)
                .isEqualTo(OperationResult.Failure(DomainError.ScanAborted(DomainError.PermissionRevoked)))
        }
}
