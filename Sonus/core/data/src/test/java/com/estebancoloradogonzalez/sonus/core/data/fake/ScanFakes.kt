package com.estebancoloradogonzalez.sonus.core.data.fake

import com.estebancoloradogonzalez.sonus.core.data.id3.Id3DataSource
import com.estebancoloradogonzalez.sonus.core.data.id3.RawTrackMetadata
import com.estebancoloradogonzalez.sonus.core.data.local.saf.DiscoveredFile
import com.estebancoloradogonzalez.sonus.core.data.local.saf.SafDataSource
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.estebancoloradogonzalez.sonus.core.domain.port.DispatcherProvider
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanStateEmitter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Returns preconfigured files per tree URI, or throws [SecurityException] to simulate lost access. */
class FakeSafDataSource(
    private val filesByTree: Map<String, List<DiscoveredFile>> = emptyMap(),
    private val throwSecurity: Boolean = false,
) : SafDataSource {
    override suspend fun listAudioFiles(treeUri: String): List<DiscoveredFile> {
        if (throwSecurity) throw SecurityException("permission revoked")
        return filesByTree[treeUri].orEmpty()
    }
}

/** Returns preconfigured metadata per URI; defaults to a minimal available track. */
class FakeId3DataSource(
    private val metadataByUri: Map<String, RawTrackMetadata> = emptyMap(),
) : Id3DataSource {
    override fun readMetadata(
        uri: String,
        mimeType: String,
    ): RawTrackMetadata =
        metadataByUri[uri]
            ?: RawTrackMetadata(
                title = null,
                artistName = null,
                albumName = null,
                genreName = null,
                contentType = ContentType.UNKNOWN,
                trackNumber = null,
                releaseYear = null,
                durationMs = 1_000L,
                hasEmbeddedArtwork = false,
                availability = TrackAvailability.AVAILABLE,
            )
}

/** Records every scan-state emission. */
class RecordingScanStateEmitter : ScanStateEmitter {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    override val state: StateFlow<ScanState> = _state

    val emissions = mutableListOf<ScanState>()

    override suspend fun update(state: ScanState) {
        emissions.add(state)
        _state.value = state
    }
}

/** [DispatcherProvider] backed by a single injected test dispatcher. */
class TestDispatcherProvider(
    private val dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}
