package com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings

import app.cash.turbine.test
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.estebancoloradogonzalez.sonus.core.domain.usecase.AddSourceFolderUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.DetectSourceFolderOverlapUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveSourceFoldersUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
}
