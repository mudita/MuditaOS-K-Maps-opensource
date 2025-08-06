package com

import com.mudita.map.common.model.MyPlaceItem
import com.mudita.myplaces.repository.MyPlacesRepository
import com.mudita.myplaces.ui.MyPlacesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class MyPlacesViewModelTest {

    @MockK
    lateinit var myPlacesRepository: MyPlacesRepository

    private lateinit var viewModel: MyPlacesViewModel

    private val testCoroutineScheduler = TestCoroutineScheduler()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        viewModel = MyPlacesViewModel(myPlacesRepository)
    }

    @AfterEach
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Given initAddMyPlace is called with isEditMode = true, then state is updated accordingly`() {
        // Given
        val myPlaceItem = myPlaceWithoutTitle

        // When
        viewModel.initAddMyPlace(myPlaceItem, true)

        // Then
        assertEquals(myPlaceItem, viewModel.state.value.myPlaceItem)
        assertFalse(viewModel.state.value.isAddMyPlaceEnabled)
    }

    @Test
    fun `Given initAddMyPlace is called with MyPlaceItem without a name and isEditMode = false, then state is updated accordingly`() {
        // Given
        val myPlaceItem = myPlaceWithoutTitle

        // When
        viewModel.initAddMyPlace(myPlaceItem, false)

        // Then
        assertEquals(myPlaceItem, viewModel.state.value.myPlaceItem)
        assertFalse(viewModel.state.value.isAddMyPlaceEnabled)
    }

    @Test
    fun `Given initAddMyPlace is called with MyPlaceItem with a name and isEditMode = false, then state is updated accordingly`() {
        // Given
        val myPlaceItem = myPlaceItemWithTitle

        // When
        viewModel.initAddMyPlace(myPlaceItem, false)

        // Then
        assertEquals(myPlaceItem, viewModel.state.value.myPlaceItem)
        assertTrue(viewModel.state.value.isAddMyPlaceEnabled)
    }

    @Test
    fun `When onSearchTextChanged is called with an empty text, then all places are loaded`() {
        // Given
        coEvery { myPlacesRepository.getMyPlaces() } returns Result.success(myPlacesItems)

        // When
        viewModel.onSearchTextChanged("")

        // Then
        coVerify(exactly = 1) { myPlacesRepository.getMyPlaces() }
        assertEquals(myPlacesItems, viewModel.state.value.items)

    }

    @Test
    fun `When onSearchTextChanged is called with a non-empty text, then all places are loaded`() {
        // Given
        val searchQuery = "My"
        val items = myPlacesItems.filter { it.title?.startsWith(searchQuery) == true }
        coEvery { myPlacesRepository.searchMyPlaces(searchQuery) } returns Result.success(items)

        // When
        viewModel.onSearchTextChanged(searchQuery)

        // Then
        coVerify(exactly = 1) { myPlacesRepository.searchMyPlaces(searchQuery) }
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `When onSearchActivate is called, then the state is updated accordingly`() {
        // When
        viewModel.onSearchActivate()

        // Then
        assertTrue(viewModel.state.value.isSearchActive)
    }

    @Test
    fun `When onSearchDeactivate is called, then the state is updated accordingly`() {
        // When
        viewModel.onSearchDeactivate()

        // Then
        assertFalse(viewModel.state.value.isSearchActive)
    }

    @Test
    fun `Given getMyPlaces succeeded, then the state is updated accordingly`() {
        // Given
        coEvery { myPlacesRepository.getMyPlaces() } returns Result.success(myPlacesItems)

        // When
        viewModel.getMyPlaces()

        // Then
        coVerify(exactly = 1) { myPlacesRepository.getMyPlaces() }
        assertEquals(myPlacesItems, viewModel.state.value.items)
    }

    @Test
    fun `When addMyPlace is called, then the item is added in the repository`() {
        // Given
        val myPlaceItem = myPlaceItemWithTitle
        coEvery { myPlacesRepository.addMyPlace(myPlaceItem) } returns Result.success(myPlaceItem)

        // When
        viewModel.addMyPlace(myPlaceItem)

        // Then
        coVerify(exactly = 1) { myPlacesRepository.addMyPlace(myPlaceItem) }
    }

    @Test
    fun `When updateMyPlace is called, then the item is updated in the repository`() {
        // Given
        val myPlaceItem = myPlaceItemWithTitle
        coEvery { myPlacesRepository.updateMyPlace(myPlaceItem) } returns Result.success(Unit)

        // When
        viewModel.updateMyPlace(myPlaceItem)

        // Then
        coVerify(exactly = 1) { myPlacesRepository.updateMyPlace(myPlaceItem) }
    }

    @Test
    fun `When validate is called with an empty string, ten isAddMyPlaceEnabled is false`() {
        // When
        viewModel.initAddMyPlace(myPlaceItemWithTitle, true)
        viewModel.validate("")

        // Then
        assertFalse(viewModel.state.value.isAddMyPlaceEnabled)
    }

    @Test
    fun `When validate is called with a non-empty string, then isAddMyPlaceEnabled is true`() {
        // When
        viewModel.initAddMyPlace(myPlaceItemWithTitle, true)
        viewModel.validate("My")

        // Then
        assertTrue(viewModel.state.value.isAddMyPlaceEnabled)
    }

    @Test
    fun `When deleteMyPlace is called, then the item is deleted by the repository`() {
        // Given
        val myPlaceItem = myPlaceItemWithTitle
        viewModel.initAddMyPlace(myPlaceItem, true)
        coEvery { myPlacesRepository.deleteMyPlace(myPlaceItem) } returns Result.success(Unit)

        // When
        viewModel.deleteMyPlace()

        // Then
        coVerify(exactly = 1) { myPlacesRepository.deleteMyPlace(myPlaceItem) }
    }

    @Test
    fun `When showLeaveBottomSheet is called, then the state is updated with the correct bottom sheet type`() {
        // When
        viewModel.showLeaveBottomSheet()

        // Then
        assertEquals(
            MyPlacesViewModel.ConfirmBottomSheetType.ShowLeaveBottomSheet(),
            viewModel.state.value.confirmBottomSheetType,
        )
    }

    @Test
    fun `When showDeleteBottomSheet is called, then the state is updated with the correct bottom sheet type`() {
        // When
        viewModel.showDeleteBottomSheet()

        // Then
        assertEquals(
            MyPlacesViewModel.ConfirmBottomSheetType.ShowDeleteBottomSheet(),
            viewModel.state.value.confirmBottomSheetType,
        )
    }

    @Test
    fun `When hideBottomSheet is called, then the state is updated with null bottom sheet type`() {
        // When
        viewModel.hideBottomSheet()

        // Then
        assertNull(viewModel.state.value.confirmBottomSheetType)
    }

    private companion object {
        private val myPlaceWithoutTitle = MyPlaceItem(id = UUID.randomUUID())

        private val myPlaceItemWithTitle = MyPlaceItem(title = "My Place", id = UUID.randomUUID())

        private val myPlacesItems = listOf(myPlaceWithoutTitle, myPlaceItemWithTitle)
    }
}