package com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings

import app.cash.turbine.test
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.estebancoloradogonzalez.sonus.core.domain.usecase.AddSourceFolderUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.DetectSourceFolderOverlapUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.GetSourceFolderRemovalImpactUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveSourceFoldersUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.RemoveSourceFolderUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.RescanLibraryUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsSourceFoldersViewModelTest {
    private val observeSourceFolders = mockk<ObserveSourceFoldersUseCase>()
    private val detectSourceFolderOverlap = mockk<DetectSourceFolderOverlapUseCase>()
    private val addSourceFolder = mockk<AddSourceFolderUseCase>()
    private val getRemovalImpact = mockk<GetSourceFolderRemovalImpactUseCase>()
    private val removeSourceFolder = mockk<RemoveSourceFolderUseCase>()
    private val rescanLibrary = mockk<RescanLibraryUseCase>(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(folders: List<SourceFolder> = emptyList()): SettingsSourceFoldersViewModel {
        every { observeSourceFolders() } returns flowOf(folders)
        return SettingsSourceFoldersViewModel(
            observeSourceFolders,
            detectSourceFolderOverlap,
            addSourceFolder,
            getRemovalImpact,
            removeSourceFolder,
            rescanLibrary,
        )
    }

    private fun folder(id: Long = 1L) =
        SourceFolder(id = id, treeUri = "content://tree/$id", displayPath = "Folder $id", dateAddedMs = id)

    @Test
    fun `reflects the registered folders on observe`() {
        // Arrange + Act (AC1/AC2)
        val vm = viewModel(listOf(folder(1L), folder(2L)))

        // Assert
        assertThat(vm.uiState.value.folders).hasSize(2)
        assertThat(vm.uiState.value.hasPendingScanContent).isFalse()
    }

    @Test
    fun `emits LaunchFolderPicker on AddFolderClicked`() =
        runTest {
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.AddFolderClicked)
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.LaunchFolderPicker)
            }
        }

    @Test
    fun `notifies added and marks pending scan when the folder is registered without overlap`() =
        runTest {
            // AC1/AC6 — success path, no overlap
            coEvery { detectSourceFolderOverlap(any()) } returns false
            coEvery { addSourceFolder(any()) } returns OperationResult.Success(1L)
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.FolderPicked("content://tree/1"))
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifyFolderAdded)
            }
            assertThat(vm.uiState.value.hasPendingScanContent).isTrue()
        }

    @Test
    fun `notifies overlap and marks pending scan when the registered folder overlaps another`() =
        runTest {
            // AC4 — registered but overlapping
            coEvery { detectSourceFolderOverlap(any()) } returns true
            coEvery { addSourceFolder(any()) } returns OperationResult.Success(2L)
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.FolderPicked("content://tree/2"))
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifyOverlap)
            }
            assertThat(vm.uiState.value.hasPendingScanContent).isTrue()
        }

    @Test
    fun `notifies duplicate without marking pending scan when the folder is a duplicate`() =
        runTest {
            // AC3
            coEvery { detectSourceFolderOverlap(any()) } returns false
            coEvery { addSourceFolder(any()) } returns OperationResult.Failure(DomainError.DuplicateSourceFolder)
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.FolderPicked("content://tree/1"))
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifyDuplicate)
            }
            assertThat(vm.uiState.value.hasPendingScanContent).isFalse()
        }

    @Test
    fun `notifies permission denied when the OS denies the permission`() =
        runTest {
            // AC5 — permission denied
            coEvery { detectSourceFolderOverlap(any()) } returns false
            coEvery { addSourceFolder(any()) } returns OperationResult.Failure(DomainError.PermissionDenied)
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.FolderPicked("content://tree/1"))
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifyPermissionDenied)
            }
            assertThat(vm.uiState.value.hasPendingScanContent).isFalse()
        }

    @Test
    fun `notifies selection cancelled on SelectionCancelled`() =
        runTest {
            // AC5 — cancellation
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.SelectionCancelled)
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifySelectionCancelled)
            }
        }

    @Test
    fun `raises the removal dialog with impact and not-last flag on RemoveFolderClicked`() =
        runTest {
            // US-006 AC1 — two folders, impact reported, not the last one
            coEvery { getRemovalImpact(1L) } returns 7
            val vm = viewModel(listOf(folder(1L), folder(2L)))

            vm.onCommand(SettingsSourceFoldersCommand.RemoveFolderClicked(1L, "Folder 1"))

            val pending = vm.uiState.value.pendingRemoval
            assertThat(pending).isNotNull()
            assertThat(pending!!.trackCount).isEqualTo(7)
            assertThat(pending.isLastFolder).isFalse()
        }

    @Test
    fun `flags the removal dialog as last folder when only one remains`() =
        runTest {
            // US-006 AC8 — removing the only folder empties the library
            coEvery { getRemovalImpact(1L) } returns 3
            val vm = viewModel(listOf(folder(1L)))

            vm.onCommand(SettingsSourceFoldersCommand.RemoveFolderClicked(1L, "Folder 1"))

            assertThat(vm.uiState.value.pendingRemoval?.isLastFolder).isTrue()
        }

    @Test
    fun `removes and notifies clearing the dialog on RemoveFolderConfirmed`() =
        runTest {
            // US-006 AC1/AC2 — confirmed removal
            coEvery { getRemovalImpact(1L) } returns 2
            coEvery { removeSourceFolder(any()) } returns OperationResult.Success(Unit)
            val vm = viewModel(listOf(folder(1L), folder(2L)))
            vm.onCommand(SettingsSourceFoldersCommand.RemoveFolderClicked(1L, "Folder 1"))

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.RemoveFolderConfirmed)
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifyFolderRemoved)
            }
            assertThat(vm.uiState.value.pendingRemoval).isNull()
        }

    @Test
    fun `notifies failure when the folder is gone on RemoveFolderConfirmed`() =
        runTest {
            // US-006 AC9 — ERR_ENTITY_NOT_FOUND
            coEvery { getRemovalImpact(1L) } returns 0
            coEvery { removeSourceFolder(any()) } returns
                OperationResult.Failure(DomainError.EntityNotFound("SourceFolder", 1L))
            val vm = viewModel(listOf(folder(1L)))
            vm.onCommand(SettingsSourceFoldersCommand.RemoveFolderClicked(1L, "Folder 1"))

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.RemoveFolderConfirmed)
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifyRemoveFailed)
            }
            assertThat(vm.uiState.value.pendingRemoval).isNull()
        }

    @Test
    fun `dismisses the removal dialog without effect on RemoveFolderDismissed`() =
        runTest {
            // US-006 AC6 — cancellation leaves everything intact
            coEvery { getRemovalImpact(1L) } returns 5
            val vm = viewModel(listOf(folder(1L)))
            vm.onCommand(SettingsSourceFoldersCommand.RemoveFolderClicked(1L, "Folder 1"))

            vm.onCommand(SettingsSourceFoldersCommand.RemoveFolderDismissed)

            assertThat(vm.uiState.value.pendingRemoval).isNull()
        }

    @Test
    fun `enqueues an incremental rescan clearing pending scan on RescanClicked with folders`() =
        runTest {
            // US-007 AC1 — manual re-scan with at least one folder configured
            val vm = viewModel(listOf(folder(1L)))

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.RescanClicked)
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifyRescanStarted)
            }
            assertThat(vm.uiState.value.hasPendingScanContent).isFalse()
            verify(exactly = 1) { rescanLibrary(ScanMode.INCREMENTAL) }
        }

    @Test
    fun `notifies no folders and does not enqueue on RescanClicked without folders`() =
        runTest {
            // US-007 — the scan needs at least one source folder (US-005 precondition)
            val vm = viewModel(emptyList())

            vm.events.test {
                vm.onCommand(SettingsSourceFoldersCommand.RescanClicked)
                assertThat(awaitItem()).isEqualTo(SettingsSourceFoldersEvent.NotifyNoFoldersToScan)
            }
            verify(exactly = 0) { rescanLibrary(any()) }
        }
}
