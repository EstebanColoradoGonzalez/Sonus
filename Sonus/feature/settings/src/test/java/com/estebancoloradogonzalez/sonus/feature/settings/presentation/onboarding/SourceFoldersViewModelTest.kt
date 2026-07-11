package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

import app.cash.turbine.test
import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.estebancoloradogonzalez.sonus.core.domain.usecase.AddSourceFolderUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveSourceFoldersUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.RemoveSourceFolderUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.VerifySourceFoldersReadyUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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
class SourceFoldersViewModelTest {
    private val observeSourceFolders = mockk<ObserveSourceFoldersUseCase>()
    private val addSourceFolder = mockk<AddSourceFolderUseCase>()
    private val removeSourceFolder = mockk<RemoveSourceFolderUseCase>()
    private val verifySourceFoldersReady = mockk<VerifySourceFoldersReadyUseCase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(folders: List<SourceFolder> = emptyList()): SourceFoldersViewModel {
        every { observeSourceFolders() } returns flowOf(folders)
        return SourceFoldersViewModel(
            observeSourceFolders,
            addSourceFolder,
            removeSourceFolder,
            verifySourceFoldersReady,
        )
    }

    private fun folder(id: Long = 1L) =
        SourceFolder(id = id, treeUri = "content://tree/$id", displayPath = "Folder $id", dateAddedMs = id)

    @Test
    fun `reflects folders and enables continue on observe`() {
        // Arrange + Act (AC1/AC2)
        val vm = viewModel(listOf(folder(1L), folder(2L)))

        // Assert
        assertThat(vm.uiState.value.folders).hasSize(2)
        assertThat(vm.uiState.value.canContinue).isTrue()
    }

    @Test
    fun `keeps continue disabled when the list is empty`() {
        // Arrange + Act (AC3)
        val vm = viewModel(emptyList())

        // Assert
        assertThat(vm.uiState.value.folders).isEmpty()
        assertThat(vm.uiState.value.canContinue).isFalse()
    }

    @Test
    fun `emits LaunchFolderPicker on AddFolderClicked`() =
        runTest {
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SourceFoldersCommand.AddFolderClicked)
                assertThat(awaitItem()).isEqualTo(SourceFoldersEvent.LaunchFolderPicker)
            }
        }

    @Test
    fun `emits no notice when the folder is added successfully`() =
        runTest {
            // AC1 — success path
            coEvery { addSourceFolder(any()) } returns OperationResult.Success(1L)
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SourceFoldersCommand.FolderPicked("content://tree/1"))
                expectNoEvents()
            }
        }

    @Test
    fun `emits NotifyDuplicate when the folder is a duplicate`() =
        runTest {
            // AC4
            coEvery { addSourceFolder(any()) } returns OperationResult.Failure(DomainError.DuplicateSourceFolder)
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SourceFoldersCommand.FolderPicked("content://tree/1"))
                assertThat(awaitItem()).isEqualTo(SourceFoldersEvent.NotifyDuplicate)
            }
        }

    @Test
    fun `emits NotifyPermissionDenied when the OS denies the permission`() =
        runTest {
            // AC5 — permission denied
            coEvery { addSourceFolder(any()) } returns OperationResult.Failure(DomainError.PermissionDenied)
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SourceFoldersCommand.FolderPicked("content://tree/1"))
                assertThat(awaitItem()).isEqualTo(SourceFoldersEvent.NotifyPermissionDenied)
            }
        }

    @Test
    fun `emits NotifySelectionCancelled on SelectionCancelled`() =
        runTest {
            // AC5 — cancellation
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SourceFoldersCommand.SelectionCancelled)
                assertThat(awaitItem()).isEqualTo(SourceFoldersEvent.NotifySelectionCancelled)
            }
        }

    @Test
    fun `emits NavigateToScan when ready on ContinueClicked`() =
        runTest {
            // AC7
            coEvery { verifySourceFoldersReady() } returns true
            val vm = viewModel(listOf(folder()))

            vm.events.test {
                vm.onCommand(SourceFoldersCommand.ContinueClicked)
                assertThat(awaitItem()).isEqualTo(SourceFoldersEvent.NavigateToScan)
            }
        }

    @Test
    fun `emits nothing when not ready on ContinueClicked`() =
        runTest {
            coEvery { verifySourceFoldersReady() } returns false
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(SourceFoldersCommand.ContinueClicked)
                expectNoEvents()
            }
        }

    @Test
    fun `invokes removeSourceFolder on RemoveFolder`() =
        runTest {
            // AC6
            coEvery { removeSourceFolder(any()) } returns OperationResult.Success(Unit)
            val vm = viewModel(listOf(folder(5L)))

            vm.onCommand(SourceFoldersCommand.RemoveFolder(5L))

            coVerify { removeSourceFolder(LibraryCommand.RemoveSourceFolder(5L)) }
        }
}
