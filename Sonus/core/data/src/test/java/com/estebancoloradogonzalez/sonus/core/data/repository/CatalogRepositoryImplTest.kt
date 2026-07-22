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
import io.mockk.coVerify
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

    private fun repository(
        saf: FakeSafDataSource,
        id3: FakeId3DataSource,
        emitter: RecordingScanStateEmitter = RecordingScanStateEmitter(),
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
    ) = CatalogRepositoryImpl(
        safDataSource = saf,
        id3DataSource = id3,
        catalogSynchronizer = synchronizer,
        scanStateEmitter = emitter,
        dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher(scheduler)),
    )

    @Test
    fun `scans every file, keeps unsupported ones and reports deterministic progress`() =
        runTest {
            // Arrange (AC3/AC7) — one available, one unsupported file under a single folder
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
            val processedSlot = slot<List<ScannedTrack>>()
            val discoveredSlot = slot<List<String>>()
            coEvery { synchronizer.sync(capture(processedSlot), capture(discoveredSlot)) } returns summary

            // Act
            val result =
                repository(saf, id3, emitter, testScheduler)
                    .synchronize(listOf(folder(1L, treeUri)), ScanMode.FULL)

            // Assert
            assertThat(result).isEqualTo(OperationResult.Success(summary))
            assertThat(processedSlot.captured).hasSize(2)
            assertThat(processedSlot.captured.map { it.availability })
                .containsExactly(TrackAvailability.AVAILABLE, TrackAvailability.UNSUPPORTED)
            assertThat(discoveredSlot.captured).containsExactly(fileA.uri, fileB.uri)
            assertThat(emitter.emissions.first()).isEqualTo(ScanState.Scanning(processed = 0, total = null))
            assertThat(emitter.emissions).contains(ScanState.Scanning(processed = 2, total = 2))
            assertThat(emitter.emissions.last()).isEqualTo(ScanState.Syncing)
        }

    @Test
    fun `returns ScanAborted when a folder loses SAF access`() =
        runTest {
            // Arrange
            val saf = FakeSafDataSource(throwSecurity = true)

            // Act
            val result =
                repository(saf, FakeId3DataSource(), scheduler = testScheduler)
                    .synchronize(listOf(folder(1L, "content://tree/x")), ScanMode.FULL)

            // Assert
            assertThat(result)
                .isEqualTo(OperationResult.Failure(DomainError.ScanAborted(DomainError.PermissionRevoked)))
        }

    @Test
    fun `skips unchanged files without re-reading their metadata when INCREMENTAL`() =
        runTest {
            // Arrange (AC1) — 'a' is unchanged (mtime == fingerprint), 'b' is new
            val treeUri = "content://tree/music"
            val fileA = DiscoveredFile("content://doc/a", "audio/mpeg", 10L)
            val fileB = DiscoveredFile("content://doc/b", "audio/mpeg", 20L)
            val saf = FakeSafDataSource(mapOf(treeUri to listOf(fileA, fileB)))
            val id3 = FakeId3DataSource(mapOf(fileB.uri to available(fileB.uri)))
            val processedSlot = slot<List<ScannedTrack>>()
            val discoveredSlot = slot<List<String>>()
            coEvery { synchronizer.indexedFingerprints() } returns mapOf(fileA.uri to 10L)
            coEvery { synchronizer.sync(capture(processedSlot), capture(discoveredSlot)) } returns
                ScanSummary(added = 1, purged = 0, unsupported = 0, orphanDimsPurged = 0)

            // Act
            repository(saf, id3, scheduler = testScheduler)
                .synchronize(listOf(folder(1L, treeUri)), ScanMode.INCREMENTAL)

            // Assert — unchanged 'a' never re-read nor processed, yet still discovered (not purged)
            assertThat(id3.readUris).containsExactly(fileB.uri)
            assertThat(processedSlot.captured.map { it.uri }).containsExactly(fileB.uri)
            assertThat(discoveredSlot.captured).containsExactly(fileA.uri, fileB.uri)
        }

    @Test
    fun `re-reads and processes a modified file when INCREMENTAL`() =
        runTest {
            // Arrange (AC2) — 'a' exists but its on-disk mtime is newer than the fingerprint
            val treeUri = "content://tree/music"
            val fileA = DiscoveredFile("content://doc/a", "audio/mpeg", 99L)
            val saf = FakeSafDataSource(mapOf(treeUri to listOf(fileA)))
            val id3 = FakeId3DataSource(mapOf(fileA.uri to available(fileA.uri)))
            val processedSlot = slot<List<ScannedTrack>>()
            coEvery { synchronizer.indexedFingerprints() } returns mapOf(fileA.uri to 10L)
            coEvery { synchronizer.sync(capture(processedSlot), any()) } returns
                ScanSummary(added = 0, purged = 0, unsupported = 0, orphanDimsPurged = 0)

            // Act
            repository(saf, id3, scheduler = testScheduler)
                .synchronize(listOf(folder(1L, treeUri)), ScanMode.INCREMENTAL)

            // Assert — the changed file is re-extracted and handed to the writer
            assertThat(id3.readUris).containsExactly(fileA.uri)
            assertThat(processedSlot.captured.map { it.uri }).containsExactly(fileA.uri)
        }

    @Test
    fun `processes a file with no fingerprint when INCREMENTAL`() =
        runTest {
            // Arrange (AC3) — 'c' is brand new: no fingerprint for its URI
            val treeUri = "content://tree/music"
            val fileC = DiscoveredFile("content://doc/c", "audio/mpeg", 5L)
            val saf = FakeSafDataSource(mapOf(treeUri to listOf(fileC)))
            val id3 = FakeId3DataSource(mapOf(fileC.uri to available(fileC.uri)))
            val processedSlot = slot<List<ScannedTrack>>()
            coEvery { synchronizer.indexedFingerprints() } returns emptyMap()
            coEvery { synchronizer.sync(capture(processedSlot), any()) } returns
                ScanSummary(added = 1, purged = 0, unsupported = 0, orphanDimsPurged = 0)

            // Act
            repository(saf, id3, scheduler = testScheduler)
                .synchronize(listOf(folder(1L, treeUri)), ScanMode.INCREMENTAL)

            // Assert
            assertThat(id3.readUris).containsExactly(fileC.uri)
            assertThat(processedSlot.captured.map { it.uri }).containsExactly(fileC.uri)
        }

    @Test
    fun `processes every discovered file without consulting fingerprints when FULL`() =
        runTest {
            // Arrange (AC8) — a file whose mtime would match a fingerprint is still fully processed
            val treeUri = "content://tree/music"
            val fileA = DiscoveredFile("content://doc/a", "audio/mpeg", 10L)
            val saf = FakeSafDataSource(mapOf(treeUri to listOf(fileA)))
            val id3 = FakeId3DataSource(mapOf(fileA.uri to available(fileA.uri)))
            val processedSlot = slot<List<ScannedTrack>>()
            coEvery { synchronizer.sync(capture(processedSlot), any()) } returns
                ScanSummary(added = 0, purged = 0, unsupported = 0, orphanDimsPurged = 0)

            // Act
            repository(saf, id3, scheduler = testScheduler)
                .synchronize(listOf(folder(1L, treeUri)), ScanMode.FULL)

            // Assert — FULL never diffs by fingerprint; every file is re-read
            assertThat(id3.readUris).containsExactly(fileA.uri)
            assertThat(processedSlot.captured.map { it.uri }).containsExactly(fileA.uri)
            coVerify(exactly = 0) { synchronizer.indexedFingerprints() }
        }
}
