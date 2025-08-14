package com.mudita.map.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.mudita.map.common.enums.MapType
import com.mudita.map.common.geocode.GeocodingAddress
import com.mudita.map.common.geocode.GeocodingAddressNotFoundException
import com.mudita.map.common.maps.GetMissingMapsUseCase
import com.mudita.map.common.maps.OnMapsReIndexedUseCase
import com.mudita.map.common.model.MyPlaceItem
import com.mudita.map.common.model.SearchItem
import com.mudita.map.common.model.SearchItemType
import com.mudita.map.common.model.navigation.NavigationItem
import com.mudita.map.common.model.navigation.NavigationPoint
import com.mudita.map.common.model.navigation.NavigationPointType
import com.mudita.map.common.model.navigation.getLatLons
import com.mudita.map.common.model.routing.RoadDetails
import com.mudita.map.common.model.routing.RouteDirectionInfo
import com.mudita.map.common.navigation.IntermediatePointReachedUseCase
import com.mudita.map.common.navigation.StopVoiceRouterUseCase
import com.mudita.map.common.repository.SettingsRepository
import com.mudita.map.common.repository.geocoding.GeocodingRepository
import com.mudita.map.common.sharedPrefs.AppFirstRunPreference
import com.mudita.map.common.sharedPrefs.MapTypesPreference
import com.mudita.map.common.sharedPrefs.SetMapLastKnownLocationUseCase
import com.mudita.map.common.utils.ChangeMapRotationModeUseCase
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.map.common.utils.VibrationFeedbackManager
import com.mudita.map.common.utils.formattedCoordinates
import com.mudita.map.repository.HistoryRepository
import com.mudita.map.repository.NavigationDirection
import com.mudita.map.repository.NavigationDisplayMode
import com.mudita.map.repository.NavigationModeItem
import com.mudita.map.repository.NavigationPointItem
import com.mudita.map.ui.routeCalculation.RouteCalculationState
import com.mudita.map.ui.routePlanning.MissingMapsState
import com.mudita.map.ui.routePlanning.RoutePlanningBottomSheetType
import com.mudita.map.usecase.CheckMemoryExceededUseCase
import com.mudita.myplaces.repository.MyPlacesRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.router.TurnType
import net.osmand.router.errors.RouteCalculationError
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class MapViewModelTest {

    @MockK
    private lateinit var myPlacesRepository: MyPlacesRepository
    @MockK
    private lateinit var geocodingRepository: GeocodingRepository
    @MockK
    private lateinit var mapTypesPreference: MapTypesPreference
    @MockK
    private lateinit var osmAndFormatter: OsmAndFormatter
    @MockK
    private lateinit var settingsRepository: SettingsRepository
    @MockK
    private lateinit var historyRepository: HistoryRepository
    @MockK
    private lateinit var vibrationFeedbackManager: VibrationFeedbackManager
    private lateinit var intermediatePointReachedUseCase: IntermediatePointReachedUseCase
    private val intermediatePointReached = MutableSharedFlow<Int>()
    @MockK(relaxed = true)
    private lateinit var stopVoiceRouterUseCase: StopVoiceRouterUseCase
    @MockK
    private lateinit var getMissingMapsUseCase: GetMissingMapsUseCase
    private lateinit var onMapsReIndexedUseCase: OnMapsReIndexedUseCase
    private val onMapsReIndexed = MutableSharedFlow<Unit>()
    @MockK(relaxed = true)
    private lateinit var setMapLastKnownLocationUseCase: SetMapLastKnownLocationUseCase
    @MockK(relaxed = true)
    private lateinit var changeMapRotationModeUseCase: ChangeMapRotationModeUseCase
    @MockK
    private lateinit var checkMemoryExceededUseCase: CheckMemoryExceededUseCase

    private val appFirstRunPreference: AppFirstRunPreference = mockk {
        every { isAppOpenedFirstTime() } returns false
    }

    private val savedStateHandle = SavedStateHandle()

    private val testCoroutineScheduler = TestCoroutineScheduler()

    private val ioDispatcher = UnconfinedTestDispatcher(testCoroutineScheduler)

    private lateinit var mapViewModel: MapViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        intermediatePointReachedUseCase = object : IntermediatePointReachedUseCase {
            override fun invoke(): Flow<Int> = intermediatePointReached
        }
        onMapsReIndexedUseCase = object : OnMapsReIndexedUseCase {
            override fun invoke(): Flow<Unit> = onMapsReIndexed
        }
        mapViewModel = MapViewModel(
            myPlacesRepository,
            geocodingRepository,
            mapTypesPreference,
            appFirstRunPreference,
            osmAndFormatter,
            settingsRepository,
            historyRepository,
            vibrationFeedbackManager,
            savedStateHandle,
            intermediatePointReachedUseCase,
            stopVoiceRouterUseCase,
            getMissingMapsUseCase,
            onMapsReIndexedUseCase,
            setMapLastKnownLocationUseCase,
            changeMapRotationModeUseCase,
            checkMemoryExceededUseCase,
            ioDispatcher,
        )
        coEvery { geocodingRepository.searchAddress(any()) } returns Result.failure(Exception())
        every { osmAndFormatter.getFormattedDistanceValue(any(), any()) } returns OsmAndFormatter.FormattedValue("", "")
        coEvery { getMissingMapsUseCase(any<List<LatLon>>()) } returns Result.success(emptyList())
        coEvery { checkMemoryExceededUseCase() } returns false
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given the app is opened for the first time, when mapViewModel is initialized, then should change screen state to WelcomeToMaps`() = runTest {
        // Given
        every { appFirstRunPreference.isAppOpenedFirstTime() } returns true

        // when
        mapViewModel = MapViewModel(
            myPlacesRepository,
            geocodingRepository,
            mapTypesPreference,
            appFirstRunPreference,
            osmAndFormatter,
            settingsRepository,
            historyRepository,
            vibrationFeedbackManager,
            savedStateHandle,
            intermediatePointReachedUseCase,
            stopVoiceRouterUseCase,
            getMissingMapsUseCase,
            onMapsReIndexedUseCase,
            setMapLastKnownLocationUseCase,
            changeMapRotationModeUseCase,
            checkMemoryExceededUseCase,
            ioDispatcher,
        )

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.WelcomeToMaps)
    }

    @Test
    fun `Given the app was already opened before, when mapViewModel is initialized, then should not change screen state`() = runTest {
        // Given
        every { appFirstRunPreference.isAppOpenedFirstTime() } returns false

        // when
        mapViewModel = MapViewModel(
            myPlacesRepository,
            geocodingRepository,
            mapTypesPreference,
            appFirstRunPreference,
            osmAndFormatter,
            settingsRepository,
            historyRepository,
            vibrationFeedbackManager,
            savedStateHandle,
            intermediatePointReachedUseCase,
            stopVoiceRouterUseCase,
            getMissingMapsUseCase,
            onMapsReIndexedUseCase,
            setMapLastKnownLocationUseCase,
            changeMapRotationModeUseCase,
            checkMemoryExceededUseCase,
            ioDispatcher,
        )

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.Idle)
    }

    @Test
    fun `Given welcome screen is displayed and gps is turned on in system settings, when closeWelcomeScreen is called, then should change screen state to Idle and update preference`() = runTest {
        // Given
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.WelcomeToMaps) }
        justRun { appFirstRunPreference.setAppOpened() }

        // When
        mapViewModel.closeWelcomeScreen(shouldShowLocationSharingDialog = false)

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertTrue(screenState is ScreenState.Idle)
        assertEquals(ScreenState.Idle.DialogType.None, (screenState as ScreenState.Idle).dialogType)
        verify(exactly = 1) { appFirstRunPreference.setAppOpened() }
    }

    @Test
    fun `Given welcome screen is displayed and gps is turned off in system settings, when closeWelcomeScreen is called, then should change screen state to Idle with Location Sharing dialog and update preference`() = runTest {
        // Given
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.WelcomeToMaps) }
        justRun { appFirstRunPreference.setAppOpened() }

        // When
        mapViewModel.closeWelcomeScreen(shouldShowLocationSharingDialog = true)

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertTrue(screenState is ScreenState.Idle)
        assertEquals(ScreenState.Idle.DialogType.LocationSharing, (screenState as ScreenState.Idle).dialogType)
        verify(exactly = 1) { appFirstRunPreference.setAppOpened() }
    }

    private fun startNavigation(addresses: List<String>) {
        every { osmAndFormatter.getFormattedTime(0f) } returns "0 sec"
        every { osmAndFormatter.getFormattedDistanceValue(0f) } returns OsmAndFormatter.FormattedValue("0", "m", false)
        mapViewModel.prepareForNavigation(
            item = NavigationItem(
                startPoint = NavigationPoint(getDummyLatLon(), ""),
                intermediatePoints = addresses.map { NavigationPoint(getDummyLatLon(), it) },
                endPoint = NavigationPoint(getDummyLatLon(), ""),
            ),
            myLocation = getDummyLatLon(),
        )
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = false)) }
    }

    @Test
    fun `When route directions get updated without a previously existing intermediate point, screen state changes`() = runTest {
        val addresses = listOf("Address 1", "Address 2", "Address 3")
        // Given
        startNavigation(addresses)

        val routeDirections = listOf(
            RouteDirectionInfo(0f, TurnType.straight()),
            RouteDirectionInfo(0f, TurnType.straight()).apply { isIntermediatePoint = true },
            RouteDirectionInfo(0f, TurnType.straight()),
            RouteDirectionInfo(0f, TurnType.straight()).apply { isIntermediatePoint = true },
            RouteDirectionInfo(0f, TurnType.straight()).apply { isIntermediatePoint = true },
            RouteDirectionInfo(0f, TurnType.straight()),
        )

        mapViewModel.updateNavigationProperties(0, 0, 0, routeDirections) { EMPTY_LOCATION }

        // When
        addresses.forEachIndexed { index, address ->
            intermediatePointReached.emit(index)

            // Then
            val screenState = mapViewModel.uiState.value.screenState
            assertInstanceOf(ScreenState.StopPoint::class.java, screenState)
            assertEquals(address, (screenState as ScreenState.StopPoint).address)

            mapViewModel.uiState.update { it.copy(screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = false)) }
        }
    }

    @Test
    fun `When screen state is StopPoint, it is dismissed automatically after a delay`() = runTest {
        val addresses = listOf("Address 1")
        // Given
        startNavigation(addresses)

        // When
        intermediatePointReached.emit(0)
        val screenState = mapViewModel.uiState.value.screenState
        assertEquals(addresses[0], (screenState as ScreenState.StopPoint).address)
        advanceTimeBy(MapViewModel.POINT_REACHED_AUTO_CLOSE_DELAY + 1)

        // Then
        assertInstanceOf(ScreenState.NavigationInProgress::class.java, mapViewModel.uiState.value.screenState)
    }

    @Test
    fun `onRouteLoading should set screen state to CalculatingRoute and search item should be cleared`() = runTest {
        // Given
        val searchItem = SearchItem(
            address = DESTINATION_POINT_ADDRESS,
            distance = START_DESTINATION_DISTANCE_METERS,
            latLon = LAT_LON_DESTINATION_POINT,
            itemType = SearchItemType.ADDRESS,
        )

        mapViewModel.routeState.update { it.copy(searchItem = searchItem) }
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.SearchResult(searchItem)) }
        mapViewModel.prepareForNavigation(getDummyNavigationItem(), null)

        // When
        mapViewModel.onRouteLoading()

        // Then
        val uiState = mapViewModel.uiState.value
        assertNull(mapViewModel.routeState.value.searchItem)
        assertTrue(uiState.screenState is ScreenState.PlanningRoute)
        assertTrue((uiState.screenState as ScreenState.PlanningRoute).routeCalculationState is RouteCalculationState.InProgress)
        assertFalse(uiState.showMapButtons)
        assertFalse(uiState.isNavigating)
    }

    @Test
    fun `onRouteLoading should should not change state to CalculatingRoute if navigation is in progress`() = runTest {
        // Given
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = false)) }

        // When
        mapViewModel.onRouteLoading()

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.NavigationInProgress)
    }

    @Test
    fun `onRouteLoading should should not change state to CalculatingRoute if navigation is in stop point`() = runTest {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.StopPoint(
                    address = INTERMEDIATE_POINT_ADDRESS,
                    isCenterButtonVisible = false
                )
            )
        }

        // When
        mapViewModel.onRouteLoading()

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.StopPoint)
    }

    @Test
    fun `onRouteLoading should should not change state to CalculatingRoute if navigation is in destination point`() = runTest {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.DestinationReached(
                    address = INTERMEDIATE_POINT_ADDRESS,
                    isCenterButtonVisible = false
                )
            )
        }

        // When
        mapViewModel.onRouteLoading()

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.DestinationReached)
    }

    @Test
    fun `updateNavigationProperties should set empty list if parameter is an empty list`() = runTest {
        // Given
        every { osmAndFormatter.getFormattedTime(0f) } returns "0 s"
        every { osmAndFormatter.getFormattedDistanceValue(0f) } returns OsmAndFormatter.FormattedValue("0", "m", false)
        mapViewModel.routeState.update { it.copy(navigationSteps = listOf(NavigationStep("", RoadDetails(), 0, null, false))) }
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.Idle,
                    routeCalculationState = RouteCalculationState.InProgress(),
                )
            )
        }

        // When
        mapViewModel.updateNavigationProperties(0, 0, 0, null) { EMPTY_LOCATION }

        // Then
        assertTrue(mapViewModel.routeState.value.navigationSteps.isEmpty())
    }

    @Test
    fun `updateNavigationProperties should set navigationSteps in routeState`() = runTest {
        // Given
        every { osmAndFormatter.getFormattedTime(0f) } returns "0 s"
        every { osmAndFormatter.getFormattedDistanceValue(0f) } returns OsmAndFormatter.FormattedValue("0", "m", false)
        mapViewModel.routeState.update { it.copy(navigationSteps = emptyList()) }
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.Idle,
                    routeCalculationState = RouteCalculationState.InProgress(),
                )
            )
        }

        // When
        val routeDirections = listOf(
            RouteDirectionInfo(averageSpeed = 50f, turnType = TurnType.straight()),
            RouteDirectionInfo(averageSpeed = 40f, turnType = TurnType.straight())
        )
        mapViewModel.updateNavigationProperties(0, 0, 0, routeDirections) { EMPTY_LOCATION }

        // Then
        assertEquals(routeDirections.size, mapViewModel.routeState.value.navigationSteps.size)
    }


    @Test
    fun `updateNavigationProperties should not change state to NavigationInProgress if navigationSteps are empty`() = runTest {
        // Given
        mapViewModel.routeState.update { it.copy(navigationSteps = emptyList()) }
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.Idle,
                    routeCalculationState = RouteCalculationState.InProgress(),
                )
            )
        }

        // When
        mapViewModel.updateNavigationProperties(0, 0, 0, emptyList()) { EMPTY_LOCATION }

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.PlanningRoute)
    }

    @Test
    fun `updateNavigationProperties should not change state to NavigationInProgress if there are missing maps`() = runTest {
        // Given
        mapViewModel.routeState.update { it.copy(missingMaps = MISSING_MAPS) }
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.MissingMapsFound(MISSING_MAPS),
                )
            )
        }

        // When
        mapViewModel.updateNavigationProperties(0, 0, 0, listOf(RouteDirectionInfo(0f, TurnType.straight()))) { EMPTY_LOCATION }

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.PlanningRoute)
    }

    @Test
    fun `updateNavigationProperties should not change state to NavigationInProgress if navigation is currently in stop point`() = runTest {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.StopPoint(
                    address = INTERMEDIATE_POINT_ADDRESS,
                    isCenterButtonVisible = false
                )
            )
        }

        // When
        mapViewModel.updateNavigationProperties(0, 0, 0, listOf(RouteDirectionInfo(0f, TurnType.straight()))) { EMPTY_LOCATION }

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.StopPoint)
    }

    @Test
    fun `updateNavigationProperties should not change state to NavigationInProgress if navigation is currently in destination point`() = runTest {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.DestinationReached(
                    address = DESTINATION_POINT_ADDRESS,
                    isCenterButtonVisible = false
                )
            )
        }

        // When
        mapViewModel.updateNavigationProperties(0, 0, 0, listOf(RouteDirectionInfo(0f, TurnType.straight()))) { EMPTY_LOCATION }

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.DestinationReached)
    }

    @Test
    fun `updateNavigationProperties should change state to NavigationInProgress if there are no missing maps and route directions are not empty`() =
        runTest {
            // Given
            every { osmAndFormatter.getFormattedTime(0f) } returns "0 s"
            every { osmAndFormatter.getFormattedDistanceValue(0f) } returns OsmAndFormatter.FormattedValue("0", "m", false)
            val routeDirections = listOf(
                RouteDirectionInfo(averageSpeed = 50f, turnType = TurnType.straight()),
                RouteDirectionInfo(averageSpeed = 40f, turnType = TurnType.straight())
            )
            mapViewModel.routeState.update { it.copy(missingMaps = emptyList()) }
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = getDummyNavigationItem(),
                        selectedNavigationMode = NavigationModeItem.Driving,
                        bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                        missingMapsState = MissingMapsState.Idle,
                        routeCalculationState = RouteCalculationState.InProgress(),
                    )
                )
            }

            // When
            mapViewModel.updateNavigationProperties(0, 0, 0, routeDirections) { EMPTY_LOCATION }

            // Then
            assertTrue(mapViewModel.uiState.value.screenState is ScreenState.NavigationInProgress)
        }

    @Test
    fun `given user is calculating route, when goBackToPlanRoute called with missing maps, then should update state with missing maps`() = runTest {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.Idle,
                    routeCalculationState = RouteCalculationState.InProgress(),
                )
            )
        }
        mapViewModel.prepareForNavigation(getDummyNavigationItem(), null)

        // When
        mapViewModel.goBackToPlanRoute(MISSING_MAPS)

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertTrue(screenState is ScreenState.PlanningRoute)
        assertInstanceOf(MissingMapsState.MissingMapsFound::class.java, (screenState as ScreenState.PlanningRoute).missingMapsState)
        assertEquals(MISSING_MAPS, (screenState.missingMapsState as MissingMapsState.MissingMapsFound).missingMaps)
        assertEquals(MISSING_MAPS, mapViewModel.routeState.value.missingMaps)
    }

    @Test
    fun `onContinueRouteClick should change state to NavigationInProgress`() = runTest {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.StopPoint(
                    address = INTERMEDIATE_POINT_ADDRESS,
                    isCenterButtonVisible = false
                )
            )
        }

        // When
        mapViewModel.onContinueRouteClick()

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.NavigationInProgress)
    }

    @Test
    fun `Given isCenterButtonVisible set to false, when onContinueRouteClick, then should change state to NavigationInProgress preserving isCenterButtonVisible state`() = runTest {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.StopPoint(
                    address = INTERMEDIATE_POINT_ADDRESS,
                    isCenterButtonVisible = false
                )
            )
        }

        // When
        mapViewModel.onContinueRouteClick()

        // Then
        assertFalse((mapViewModel.uiState.value.screenState as ScreenState.NavigationInProgress).isCenterButtonVisible)
    }

    @Test
    fun `Given isCenterButtonVisible set to true, when onContinueRouteClick, then should change state to NavigationInProgress preserving isCenterButtonVisible state`() = runTest {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.StopPoint(
                    address = INTERMEDIATE_POINT_ADDRESS,
                    isCenterButtonVisible = true
                )
            )
        }

        // When
        mapViewModel.onContinueRouteClick()

        // Then
        assertTrue((mapViewModel.uiState.value.screenState as ScreenState.NavigationInProgress).isCenterButtonVisible)
    }

    @Test
    fun `onDestinationReached should change screen state to DestinationReached with correct address and trigger vibration`() = runTest {
        // Given
        mapViewModel.prepareForNavigation(
            NavigationItem(
                startPoint = NavigationPoint(LAT_LON_START_POINT, ""),
                endPoint = NavigationPoint(LAT_LON_DESTINATION_POINT, DESTINATION_POINT_ADDRESS)
            ),
            LAT_LON_START_POINT,
        )
        justRun { vibrationFeedbackManager.vibrate() }

        mapViewModel.routeState.update { it.copy(finalLocation = LAT_LON_DESTINATION_POINT) }
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = false)) }

        // When
        mapViewModel.onDestinationReached { }

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.DestinationReached)
        assertEquals(DESTINATION_POINT_ADDRESS, (mapViewModel.uiState.value.screenState as ScreenState.DestinationReached).address)
        verify(exactly = 1) { vibrationFeedbackManager.vibrate() }
    }

    @Test
    fun `onDestinationReached should change screen state to DestinationReached and then after 5 seconds should reset state`() = runTest {
        // Given
        mapViewModel.prepareForNavigation(
            NavigationItem(
                startPoint = NavigationPoint(LAT_LON_START_POINT, ""),
                endPoint = NavigationPoint(LAT_LON_DESTINATION_POINT, DESTINATION_POINT_ADDRESS)
            ),
            LAT_LON_START_POINT,
        )
        justRun { vibrationFeedbackManager.vibrate() }

        mapViewModel.routeState.update {
            it.copy(
                finalLocation = LAT_LON_DESTINATION_POINT,
                navigationSteps = listOf(
                    NavigationStep("", RoadDetails(), 0, null, false),
                    NavigationStep("", RoadDetails(), 0, null, false),
                ),
                estimatedRouteTime = NavigationTime.HoursMinutes(2, 0),
                estimatedRouteDistance = START_DESTINATION_DISTANCE,
                searchItem = mockk(),
                navigationItem = mockk(),
            )
        }
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = false),
                navigationDisplayMode = NavigationDisplayMode.Commands,
                isSoundEnabled = true,
            )
        }

        // When
        mapViewModel.onDestinationReached { }

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.DestinationReached)

        advanceUntilIdle() // after 5 seconds

        val expectedClearedUiState = UiState(
            screenState = ScreenState.Idle(),
            navigationDisplayMode = NavigationDisplayMode.Map,
            isSoundEnabled = true,
            isNavigationBackHandlerEnabled = true,
        )
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.Idle)
        assertEquals(expectedClearedUiState, mapViewModel.uiState.value)
        assertEquals(RouteState(), mapViewModel.routeState.value)
    }

    @Test
    fun `onDestinationReached without final location shouldn't change state`() = runTest {
        // Given
        mapViewModel.routeState.update { it.copy(finalLocation = null) }
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = false)) }

        // When
        mapViewModel.onDestinationReached { }

        // Then
        assertTrue(mapViewModel.uiState.value.screenState is ScreenState.NavigationInProgress)
        verify(exactly = 0) { vibrationFeedbackManager.vibrate() }
    }

    @Test
    fun `Given isCenterButtonVisible set to true, when destination is reached, then should change state to DestinationReached preserving isCenterButtonVisible state`() = runTest {
        // Given
        mapViewModel.prepareForNavigation(
            NavigationItem(
                startPoint = NavigationPoint(LAT_LON_START_POINT, ""),
                endPoint = NavigationPoint(LAT_LON_DESTINATION_POINT, DESTINATION_POINT_ADDRESS)
            ),
            LAT_LON_START_POINT,
        )
        justRun { vibrationFeedbackManager.vibrate() }

        mapViewModel.routeState.update {
            it.copy(
                finalLocation = LAT_LON_DESTINATION_POINT,
                navigationSteps = listOf(
                    NavigationStep("", RoadDetails(), 0, null, false),
                    NavigationStep("", RoadDetails(), 0, null, false),
                ),
                estimatedRouteTime = NavigationTime.HoursMinutes(2, 0),
                estimatedRouteDistance = START_DESTINATION_DISTANCE,
            )
        }
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = true)) }

        // When
        mapViewModel.onDestinationReached { }

        // Then
        assertTrue((mapViewModel.uiState.value.screenState as ScreenState.DestinationReached).isCenterButtonVisible)
    }

    @Test
    fun `Given isCenterButtonVisible set to false, when destination is reached, then should change state to DestinationReached preserving isCenterButtonVisible state`() = runTest {
        // Given
        mapViewModel.prepareForNavigation(
            NavigationItem(
                startPoint = NavigationPoint(LAT_LON_START_POINT, ""),
                endPoint = NavigationPoint(LAT_LON_DESTINATION_POINT, DESTINATION_POINT_ADDRESS)
            ),
            LAT_LON_START_POINT,
        )
        justRun { vibrationFeedbackManager.vibrate() }

        mapViewModel.routeState.update {
            it.copy(
                finalLocation = LAT_LON_DESTINATION_POINT,
                navigationSteps = listOf(
                    NavigationStep("", RoadDetails(), 0, null, false),
                    NavigationStep("", RoadDetails(), 0, null, false),
                ),
                estimatedRouteTime = NavigationTime.HoursMinutes(2, 0),
                estimatedRouteDistance = START_DESTINATION_DISTANCE,
            )
        }
        mapViewModel.uiState.update { it.copy(screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = false)) }

        // When
        mapViewModel.onDestinationReached { }

        // Then
        assertFalse((mapViewModel.uiState.value.screenState as ScreenState.DestinationReached).isCenterButtonVisible)
    }

    @Test
    fun `given NavigationDisplayMode set to Commands, then onNavigationDisplayModeChanged should change it to Map`() = runTest {
        // Given
        mapViewModel.uiState.update { it.copy(navigationDisplayMode = NavigationDisplayMode.Commands) }

        // When
        mapViewModel.onNavigationDisplayModeChanged()

        // Then
        assertTrue(mapViewModel.uiState.value.navigationDisplayMode is NavigationDisplayMode.Map)
    }

    @Test
    fun `given NavigationDisplayMode set to Map, then onNavigationDisplayModeChanged should change it to Command`() = runTest {
        // Given
        mapViewModel.uiState.update { it.copy(navigationDisplayMode = NavigationDisplayMode.Map) }

        // When
        mapViewModel.onNavigationDisplayModeChanged()

        // Then
        assertTrue(mapViewModel.uiState.value.navigationDisplayMode is NavigationDisplayMode.Commands)
    }

    @Test
    fun `given isSoundEnabled set to false, then onSoundClicked should change it to true and save value in repository`() = runTest {
        // Given
        mapViewModel.uiState.update { it.copy(isSoundEnabled = false) }
        justRun { settingsRepository.saveSoundEnabled(any()) }

        // When
        mapViewModel.onSoundClicked()

        // Then
        assertTrue(mapViewModel.uiState.value.isSoundEnabled)
        verify(exactly = 1) { settingsRepository.saveSoundEnabled(true) }
    }

    @Test
    fun `given isSoundEnabled set to true, then onSoundClicked should change it to false and save value in repository`() = runTest {
        // Given
        mapViewModel.uiState.update { it.copy(isSoundEnabled = true) }
        justRun { settingsRepository.saveSoundEnabled(any()) }

        // When
        mapViewModel.onSoundClicked()

        // Then
        assertFalse(mapViewModel.uiState.value.isSoundEnabled)
        verify(exactly = 1) { settingsRepository.saveSoundEnabled(false) }
    }

    @Test
    fun `goBackToPlanRoute should update state with PlanningRoute`() = runTest {
        // Given
        mapViewModel.prepareForNavigation(getDummyNavigationItem(), getDummyLatLon())
        mapViewModel.updateNavigationProperties(0, 0, 0, listOf(RouteDirectionInfo(0f, TurnType.straight()))) { EMPTY_LOCATION }

        // When
        mapViewModel.goBackToPlanRoute()

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertInstanceOf(ScreenState.PlanningRoute::class.java, screenState)
        assertEquals(RoutePlanningBottomSheetType.NavigationModeSelection, (screenState as ScreenState.PlanningRoute).bottomSheetType)
    }

    @Test
    fun `goBackToPlanRoute should reset navigationDisplayMode to Map`() = runTest {
        // Given
        mapViewModel.prepareForNavigation(getDummyNavigationItem(), getDummyLatLon())
        mapViewModel.updateNavigationProperties(0, 0, 0, listOf(RouteDirectionInfo(0f, TurnType.straight()))) { EMPTY_LOCATION }
        mapViewModel.onNavigationDisplayModeChanged()

        // When
        mapViewModel.goBackToPlanRoute()

        // Then
        assertEquals(NavigationDisplayMode.Map, mapViewModel.uiState.value.navigationDisplayMode)
    }

    @Test
    fun `setCurrentLocation should update state with current location`() = runTest {
        // Given
        mapViewModel.routeState.update { it.copy(currentLocation = null, finalLocation = null) }

        // When
        mapViewModel.setCurrentLocation(LAT_LON_START_POINT)

        // Then
        assertEquals(LAT_LON_START_POINT, mapViewModel.routeState.value.currentLocation)
        assertNull(mapViewModel.routeState.value.finalLocation)
    }

    @Test
    fun `setFinalAndCurrentLocation should update state with current and final location`() = runTest {
        // Given
        mapViewModel.routeState.update { it.copy(currentLocation = null, finalLocation = null) }

        // When
        mapViewModel.setFinalAndCurrentLocation(LAT_LON_START_POINT, LAT_LON_DESTINATION_POINT)

        // Then
        assertEquals(LAT_LON_START_POINT, mapViewModel.routeState.value.currentLocation)
        assertEquals(LAT_LON_DESTINATION_POINT, mapViewModel.routeState.value.finalLocation)
    }

    @Test
    fun `updateNavigationProperties should update state with the new navigation properties`() = runTest {
        // Given
        every {
            osmAndFormatter.getFormattedDistanceValue(START_DESTINATION_DISTANCE_METERS.toFloat())
        } returns OsmAndFormatter.FormattedValue("130", "km", false)

        every {
            osmAndFormatter.getFormattedDistanceValue(NEXT_TURN_DISTANCE_METERS.toFloat())
        } returns OsmAndFormatter.FormattedValue("3", "km", false)

        every {
            osmAndFormatter.getFormattedDistanceValue(0f)
        } returns OsmAndFormatter.FormattedValue("0", "m", false)

        mapViewModel.prepareForNavigation(getDummyNavigationItem(), null)
        mapViewModel.onRouteLoading()
        mapViewModel.routeState.update { it.copy(estimatedRouteDistance = null) }

        // When
        mapViewModel.updateNavigationProperties(
            estimatedRouteDistance = START_DESTINATION_DISTANCE_METERS.toInt(),
            estimatedRouteTime = 7800, // 7800 seconds = 2h 10 min
            estimatedNextTurnDistance = NEXT_TURN_DISTANCE_METERS.toInt(),
            routeDirections = listOf(
                RouteDirectionInfo(10f, TurnType.straight()).apply { distance = NEXT_TURN_DISTANCE_METERS.toInt() },
                RouteDirectionInfo(12f, TurnType.straight()),
                RouteDirectionInfo(16f, TurnType.straight()),
            ),
        ) { EMPTY_LOCATION }

        // Then
        val routeState = mapViewModel.routeState.value
        assertEquals(START_DESTINATION_DISTANCE, routeState.estimatedRouteDistance)
        assertEquals(NavigationTime.HoursMinutes(2, 10), routeState.estimatedRouteTime)
        assertEquals(NEXT_TURN_DISTANCE, routeState.navigationSteps.first().distance)
    }

    @Test
    fun `when total estimated time is less than hour, then setLeftTotalTime should update state with total time left with minutes and seconds`() =
        runTest {
            // Given
            mapViewModel.prepareForNavigation(getDummyNavigationItem(), null)
            mapViewModel.onRouteLoading()
            mapViewModel.routeState.update { it.copy(estimatedRouteTime = null) }
            val routeTime = 3200
            val routeTimeFormatted = NavigationTime.Minutes(53)
            every { osmAndFormatter.getFormattedDistanceValue(0f) } returns OsmAndFormatter.FormattedValue("0", "m", false)

            // When
            mapViewModel.updateNavigationProperties(
                estimatedRouteDistance = 0,
                estimatedRouteTime = routeTime,
                estimatedNextTurnDistance = 0,
                routeDirections = null,
            ) { EMPTY_LOCATION }

            // Then
            assertEquals(routeTimeFormatted, mapViewModel.routeState.value.estimatedRouteTime)
        }

    @Test
    fun `when total estimated time is less than 31 seconds, then setLeftTotalTime should update state with total time left with seconds`() = runTest {
        // Given
        val expectedEstimatedRouteTime = NavigationTime.Seconds(30)
        every { osmAndFormatter.getFormattedDistanceValue(0f) } returns OsmAndFormatter.FormattedValue("0", "m", false)
        mapViewModel.prepareForNavigation(getDummyNavigationItem(), null)
        mapViewModel.onRouteLoading()
        mapViewModel.routeState.update { it.copy(estimatedRouteTime = null) }

        // When
        mapViewModel.updateNavigationProperties(
            estimatedRouteDistance = 0,
            estimatedRouteTime = 30,
            estimatedNextTurnDistance = 0,
            routeDirections = null,
        ) { EMPTY_LOCATION }

        // Then
        assertEquals(expectedEstimatedRouteTime, mapViewModel.routeState.value.estimatedRouteTime)
    }

    @Test
    fun `when selected navigation mode item is not null, setNavigationMode should select the correct navigation mode item`() = runTest {
        // Given
        every { mapTypesPreference.getMapType() } returns MapType.WALKING
        justRun { mapTypesPreference.setMapType(any()) }

        // When
        mapViewModel.setNavigationMode(NavigationModeItem.Driving)

        // Then
        assertEquals(NavigationModeItem.Driving, mapViewModel.selectedNavigationModeItem)
        verify(exactly = 1) { mapTypesPreference.setMapType(MapType.DRIVING) }
    }

    @Test
    fun `when selected navigation mode item is null, setNavigationMode should select the correct item based on map type`() = runTest {
        // Given
        val mapType = MapType.DRIVING
        every { mapTypesPreference.getMapType() } returns mapType

        // When
        mapViewModel.setNavigationMode(null)

        // Then
        assertEquals(NavigationModeItem.fromMapType(mapType), mapViewModel.selectedNavigationModeItem)
        verify(exactly = 0) { mapTypesPreference.setMapType(any()) }
    }

    @Test
    fun `initSettings should update flag isSoundEnabled`() = runTest {
        // Given
        mapViewModel.uiState.update { it.copy(isSoundEnabled = false) }
        every { settingsRepository.getSoundEnabled() } returns true

        // When
        mapViewModel.initSettings()

        // Then
        assertTrue(mapViewModel.uiState.value.isSoundEnabled)
    }

    @Test
    fun `deleteMyPlace should update route state and delete myPlace from repository`() = runTest {
        // Given
        val myPlaceToDelete: MyPlaceItem = mockk()
        mapViewModel.routeState.update { it.copy(myPlaceItem = myPlaceToDelete) }
        coJustRun { myPlacesRepository.deleteMyPlace(any()) }

        // When
        mapViewModel.deleteMyPlace(myPlaceToDelete)

        // Then
        assertNull(mapViewModel.routeState.value.myPlaceItem)
        coVerify(exactly = 1) { myPlacesRepository.deleteMyPlace(myPlaceToDelete) }
    }

    @Test
    fun `nextSteps should be null if navigationSteps is empty`() = runTest {
        // Given
        mapViewModel.routeState.update { it.copy(navigationSteps = emptyList()) }

        // Then
        mapViewModel.nextSteps.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `nextSteps should updated based on routeState`() = runTest {
        // Given
        val navigationSteps = listOf(
            NavigationStep("100m", RoadDetails(name = "Street A"), 0, null, false),
            NavigationStep("250m", RoadDetails(name = "Street B"), 0, null, false),
            NavigationStep("400m", RoadDetails(name = "Street C"), 0, null, false),
        )
        mapViewModel.routeState.update {
            it.copy(navigationSteps = navigationSteps)
        }

        // Then
        val expectedNextSteps = NextSteps(
            first = navigationSteps[0],
            second = navigationSteps[1],
        )
        mapViewModel.nextSteps.test {
            assertEquals(expectedNextSteps, awaitItem())
        }
    }

    @Test
    fun `Given Destination NavigationPoint is NOT set, add intermediate point button is disabled`() {
        val navigationItem = NavigationItem(startPoint = NavigationPoint(LAT_LON_START_POINT, START_POINT_ADDRESS))
        val navigationDirection = mapViewModel.createNavigationDirection(navigationItem = navigationItem, showAllAsInactive = false)
        assertFalse(navigationDirection.destination.isActionButtonActive)
    }

    @Test
    fun `Given Destination NavigationPoint is NOT set, adding intermediate points is impossible`() {
        val navigationItem = NavigationItem(startPoint = NavigationPoint(LAT_LON_START_POINT, START_POINT_ADDRESS))
        val updatedNavigationItem = getUpdatedNavigationItemFromViewModel(navigationItem) { destination }
        assertEquals(0, updatedNavigationItem.intermediatePoints.size)
    }

    @Test
    fun `Given Destination NavigationPoint is set, adding intermediate points is possible`() {
        val navigationItem = NavigationItem(
            startPoint = NavigationPoint(LAT_LON_START_POINT, START_POINT_ADDRESS),
            endPoint = NavigationPoint(LAT_LON_DESTINATION_POINT, DESTINATION_POINT_ADDRESS, isActionActive = true),
        )
        val updatedNavigationItem = getUpdatedNavigationItemFromViewModel(navigationItem) { destination }
        assertEquals(1, updatedNavigationItem.intermediatePoints.size)
    }

    @Test
    fun `Given Intermediate NavigationPoint is added, clicking it again removes the NavigationPoint`() {
        val navigationItem = NavigationItem(
            startPoint = NavigationPoint(LAT_LON_START_POINT, START_POINT_ADDRESS),
            intermediatePoints = listOf(NavigationPoint(LAT_LON_INTERMEDIATE_POINT, INTERMEDIATE_POINT_ADDRESS)),
            endPoint = NavigationPoint(LAT_LON_DESTINATION_POINT, DESTINATION_POINT_ADDRESS, isActionActive = true),
        )
        val updatedNavigationItem = getUpdatedNavigationItemFromViewModel(navigationItem) { intermediate.first() }
        assertEquals(0, updatedNavigationItem.intermediatePoints.size)
    }

    @Test
    fun `Backstack navigationItem shouldn't override newer version`() {
        // Given
        val firstNavigationItem = NavigationItem(
            intermediatePoints = listOf(
                NavigationPoint(
                    latLon = LAT_LON_INTERMEDIATE_POINT,
                    address = "",
                    isActionActive = true,
                    type = NavigationPointType.INTERMEDIATE
                ),
            )
        )
        val secondNavigationItem = NavigationItem(
            startPoint = NavigationPoint(
                latLon = LAT_LON_START_POINT,
                address = "",
                isActionActive = true,
                type = NavigationPointType.START
            ),
        )
        mapViewModel.onBackstackNavigationItemsChanged(firstNavigationItem, null)
        mapViewModel.setNavigationItem(secondNavigationItem)

        // When
        mapViewModel.onBackstackNavigationItemsChanged(firstNavigationItem, null)

        // Then
        assertEquals(secondNavigationItem, mapViewModel.routeState.value.navigationItem)
    }

    @Test
    fun `Given route with starting, intermediate and destination points, when 'remove' button is tapped for intermediate point, then missing maps should be checked`() =
        runTest {
            // Given
            val startPoint = NavigationPoint(
                latLon = LatLon(1.1, 0.0),
                address = "Street A",
                type = NavigationPointType.START,
                isActionActive = true
            )
            val uuid = UUID.randomUUID()
            val firstIntermediatePoint = NavigationPoint(
                uuid = uuid,
                latLon = LatLon(1.0, 0.1),
                address = "Street B",
                type = NavigationPointType.INTERMEDIATE
            )
            val endPoint = NavigationPoint(
                latLon = LatLon(1.2, 0.0),
                address = "Street D",
                type = NavigationPointType.DESTINATION,
                isActionActive = false
            )
            val navigationItem = NavigationItem(
                startPoint = startPoint,
                intermediatePoints = listOf(firstIntermediatePoint),
                endPoint = endPoint,
            )

            mapViewModel.routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                    missingMaps = MISSING_MAPS
                )
            }
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = navigationItem,
                        selectedNavigationMode = NavigationModeItem.Driving,
                        bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                        missingMapsState = MissingMapsState.MissingMapsFound(MISSING_MAPS),
                    )
                )
            }

            coEvery { getMissingMapsUseCase(any<List<LatLon>>()) } returns Result.success(emptyList())

            // When
            mapViewModel.onNavigationPointActionClicked(
                NavigationPointItem.Intermediate(uuid = uuid, intermediateAddress = "", intermediateLocation = mockk())
            )

            // Then
            val actualNavigationItem = mapViewModel.routeState.value.navigationItem
            assertTrue(actualNavigationItem?.intermediatePoints?.isEmpty() == true)

            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            assertEquals(RoutePlanningBottomSheetType.NavigationModeSelection, screenState.bottomSheetType)
            assertInstanceOf(MissingMapsState.Idle::class.java, screenState.missingMapsState)
            assertTrue(mapViewModel.routeState.value.missingMaps.isEmpty())
        }

    private fun getUpdatedNavigationItemFromViewModel(
        navigationItem: NavigationItem,
        getPoint: NavigationDirection.() -> NavigationPointItem,
    ): NavigationItem {
        mapViewModel.setNavigationItem(navigationItem)
        val navigationDirection = mapViewModel.createNavigationDirection(navigationItem = navigationItem, showAllAsInactive = false)
        mapViewModel.onNavigationPointActionClicked(getPoint(navigationDirection))
        return mapViewModel.routeState.value.navigationItem!!
    }

    @Test
    fun `Given the navigation has not been started, navigation back handler is disabled`() = runTest {
        assertFalse(mapViewModel.uiState.first().isNavigationBackHandlerEnabled)
    }

    @Test
    fun `Given the navigation has been started, navigation back handler is enabled`() = runTest {
        mapViewModel.prepareForNavigation(getDummyNavigationItem(), getDummyLatLon())
        assertTrue(mapViewModel.uiState.first().isNavigationBackHandlerEnabled)
    }

    @Test
    fun `Given the navigation has been started, plan route state is displayed on back press`() = runTest {
        // Given
        val navigationItem = getDummyNavigationItem()
        mapViewModel.prepareForNavigation(navigationItem, getDummyLatLon())

        assertNull(mapViewModel.routeState.value.navigationItem)

        // When
        mapViewModel.goBackToPlanRoute()

        // Then
        assertFalse(mapViewModel.uiState.first().isNavigationBackHandlerEnabled)
        assertEquals(navigationItem, mapViewModel.routeState.value.navigationItem)
    }

    @Test
    fun `Given the current location is set to any navigation point, the correct point is updated and others have proper active states`() {
        // Given
        val pointToUpdate = NavigationPoint(LatLon(0.0, 0.0), "Start")
        val navigationItem = NavigationItem(
            startPoint = pointToUpdate,
            intermediatePoints = List(3) { NavigationPoint(LatLon(it.toDouble(), it.toDouble()), "Intermediate $it") },
            endPoint = NavigationPoint(LatLon(0.0, 0.0), "End")
        )
        mapViewModel.setNavigationItem(navigationItem)

        // When
        val updatedNavigationItem = mapViewModel.setCurrentLocationToNavigationItem(CURRENT_POINT_LATLON)

        // Then
        assertEquals("", updatedNavigationItem.startPoint?.address)
        assertEquals(CURRENT_POINT_LATLON, updatedNavigationItem.startPoint?.latLon)
        assertTrue(updatedNavigationItem.startPoint!!.isActive)
        assertTrue(updatedNavigationItem.startPoint!!.isActionActive)

        assertNotEquals(CURRENT_POINT_ADDRESS, updatedNavigationItem.endPoint?.address)
        assertNotEquals(CURRENT_POINT_LATLON, updatedNavigationItem.endPoint?.latLon)
        assertTrue(updatedNavigationItem.endPoint!!.isActive)
        assertFalse(updatedNavigationItem.endPoint!!.isActionActive)

        updatedNavigationItem.intermediatePoints.forEach { point ->
            assertNotEquals(CURRENT_POINT_ADDRESS, point.address)
            assertNotEquals(CURRENT_POINT_LATLON, point.latLon)
            assertTrue(point.isActive)
            assertTrue(point.isActionActive)
        }
    }

    @Test
    fun `Given user is planning route, when showNavigationPointSelection called, should change bottomSheetType to NavigationPointSelection`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }

        // When
        mapViewModel.showNavigationPointSelection()

        // Then
        assertEquals(
            RoutePlanningBottomSheetType.NavigationPointSelection,
            (mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute).bottomSheetType
        )
    }

    @Test
    fun `Given user is planning route, when closeNavigationPointSelection called, should change bottomSheetType to NavigationModeSelection`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }

        // When
        mapViewModel.closeNavigationPointSelection()

        // Then
        assertEquals(
            RoutePlanningBottomSheetType.NavigationModeSelection,
            (mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute).bottomSheetType
        )
    }

    @Test
    fun `Given user is planning route, start point is selected, current location is null and there are missing maps, when closeNavigationPointSelectionAndCheckGPS called, should clear missing maps error`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = true,
            isActionActive = false,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val endPoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = false,
            isActionActive = false,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = endPoint,
            currentlySelectedPoint = startPoint
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.MissingMapsFound(MISSING_MAPS),
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem, missingMaps = MISSING_MAPS) }

        // When
        mapViewModel.updatePlanRouteStateOnCurrentLocation(null)

        // Then
        val screenState = mapViewModel.uiState.value.screenState

        assertEquals(RoutePlanningBottomSheetType.GPSError, (screenState as ScreenState.PlanningRoute).bottomSheetType)
        assertInstanceOf(MissingMapsState.Idle::class.java, screenState.missingMapsState)
        assertTrue(mapViewModel.routeState.value.missingMaps.isEmpty())
    }

    @Test
    fun `Given user is planning route, start point is selected and current location is null, when closeNavigationPointSelectionAndCheckGPS called, should change bottomSheetType to GPSError and deactivate navigation points`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = true,
            isActionActive = false,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val intermediatePoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = false,
            isActionActive = false,
            type = NavigationPointType.INTERMEDIATE,
            isCurrentLocation = false,
        )
        val endPoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = false,
            isActionActive = false,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = listOf(intermediatePoint),
            endPoint = endPoint,
            currentlySelectedPoint = startPoint
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        mapViewModel.updatePlanRouteStateOnCurrentLocation(null)

        // Then
        val screenState = mapViewModel.uiState.value.screenState

        assertEquals(RoutePlanningBottomSheetType.GPSError, (screenState as ScreenState.PlanningRoute).bottomSheetType)

        assertFalse(screenState.navigationItem.endPoint!!.isActive)
        assertFalse(mapViewModel.routeState.value.navigationItem!!.endPoint!!.isActive)
        assertFalse(screenState.navigationItem.endPoint!!.isActionActive)
        assertFalse(mapViewModel.routeState.value.navigationItem!!.endPoint!!.isActionActive)

        assertFalse(screenState.navigationItem.intermediatePoints.first().isActive)
        assertFalse(mapViewModel.routeState.value.navigationItem!!.intermediatePoints.first().isActive)
        assertFalse(screenState.navigationItem.intermediatePoints.first().isActionActive)
        assertFalse(mapViewModel.routeState.value.navigationItem!!.intermediatePoints.first().isActionActive)

        assertNotNull(screenState.navigationItem.startPoint)
        assertNotNull(mapViewModel.routeState.value.navigationItem!!.startPoint)

        assertNull(screenState.navigationItem.currentlySelectedPoint)
        assertNull(mapViewModel.routeState.value.navigationItem!!.currentlySelectedPoint)
    }

    @Test
    fun `Given user is planning route and current location is not null, when closeNavigationPointSelectionAndCheckGPS called, should change bottomSheetType to NavigationModeSelection`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = false,
            isActionActive = false,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val endPoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = true,
            isActionActive = false,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = endPoint,
            currentlySelectedPoint = endPoint
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        mapViewModel.updatePlanRouteStateOnCurrentLocation(LAT_LON_DESTINATION_POINT)

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertEquals(RoutePlanningBottomSheetType.NavigationModeSelection, (screenState as ScreenState.PlanningRoute).bottomSheetType)
    }

    @Test
    fun `Given user is planning route and is selecting navigation point on map, when onSinglePressed called and selected pin has no address defined, should update navigation point with coordinates`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LAT_LON_START_POINT,
            address = "Address",
            isActive = false,
            isActionActive = false,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val currentlySelected = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            type = NavigationPointType.DESTINATION,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = null,
            currentlySelectedPoint = currentlySelected
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.SelectLocation(
                    navigationItem = navigationItem,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        coEvery { geocodingRepository.searchAddress(any()) } returns Result.failure(GeocodingAddressNotFoundException(LAT_LON_DESTINATION_POINT))

        // When
        mapViewModel.onSinglePressed(LAT_LON_DESTINATION_POINT)

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertEquals(LAT_LON_DESTINATION_POINT.formattedCoordinates(), screenState.navigationItem.endPoint?.address)
        assertEquals(LAT_LON_DESTINATION_POINT, screenState.navigationItem.endPoint?.latLon)
        assertEquals(LAT_LON_DESTINATION_POINT.formattedCoordinates(), mapViewModel.routeState.value.navigationItem?.endPoint?.address)
        assertEquals(LAT_LON_DESTINATION_POINT, mapViewModel.routeState.value.navigationItem?.endPoint?.latLon)
    }

    @Test
    fun `Given user is planning route and is selecting navigation point on map, when onSinglePressed called and selected pin has address defined, should update navigation point with given address`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LAT_LON_START_POINT,
            address = "Address",
            isActive = false,
            isActionActive = false,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val currentlySelected = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            type = NavigationPointType.DESTINATION,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = null,
            currentlySelectedPoint = currentlySelected
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.SelectLocation(
                    navigationItem = navigationItem,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        val address = GeocodingAddress(
            city = "Warszawa",
            street = "aleja Zieleniecka",
            buildingNumber = "1",
            postcode = "03-727",
            latLon = LAT_LON_DESTINATION_POINT
        )
        coEvery { geocodingRepository.searchAddress(any()) } returns Result.success(address)

        // When
        mapViewModel.onSinglePressed(LAT_LON_DESTINATION_POINT)

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertEquals(address.streetWithBuildingNumber, screenState.navigationItem.endPoint?.address)
        assertEquals(LAT_LON_DESTINATION_POINT, screenState.navigationItem.endPoint?.latLon)
        assertEquals(address.streetWithBuildingNumber, mapViewModel.routeState.value.navigationItem?.endPoint?.address)
        assertEquals(LAT_LON_DESTINATION_POINT, mapViewModel.routeState.value.navigationItem?.endPoint?.latLon)
    }

    @Test
    fun `Given user is planning route and there are missing maps, when setNavigationItem called, should update state with missing maps`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LAT_LON_START_POINT,
            address = "Address",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val endPoint = NavigationPoint(
            latLon = LAT_LON_DESTINATION_POINT,
            address = "",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = endPoint,
        )

        coEvery { getMissingMapsUseCase(navigationItem.getLatLons()) } returns Result.success(MISSING_MAPS)

        // When
        mapViewModel.setNavigationItem(navigationItem)

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertInstanceOf(MissingMapsState.MissingMapsFound::class.java, screenState.missingMapsState)
        assertEquals(MISSING_MAPS, (screenState.missingMapsState as MissingMapsState.MissingMapsFound).missingMaps)
        assertEquals(MISSING_MAPS, mapViewModel.routeState.value.missingMaps)
    }


    @Test
    fun `Given user is planning route and there are no missing maps returned by usecase, when setNavigationItem called, should clear missing maps`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LAT_LON_START_POINT,
            address = "Address",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val endPoint = NavigationPoint(
            latLon = LAT_LON_DESTINATION_POINT,
            address = "",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = endPoint,
        )

        coEvery { getMissingMapsUseCase(navigationItem.getLatLons()) } returns Result.success(emptyList())

        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.MissingMapsFound(MISSING_MAPS),
                )
            )
        }
        mapViewModel.routeState.update {
            it.copy(
                navigationItem = mockk(),
                missingMaps = MISSING_MAPS,
            )
        }

        // When
        mapViewModel.setNavigationItem(navigationItem)

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertInstanceOf(MissingMapsState.Idle::class.java, screenState.missingMapsState)
        assertTrue(mapViewModel.routeState.value.missingMaps.isEmpty())
    }

    @Test
    fun `Given user is planning route and there is route calculation error, when setNavigationItem called, should clear this error`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LAT_LON_START_POINT,
            address = "Address",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val endPoint = NavigationPoint(
            latLon = LAT_LON_DESTINATION_POINT,
            address = "",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = endPoint,
        )

        coEvery { getMissingMapsUseCase(navigationItem.getLatLons()) } returns Result.success(emptyList())

        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.Idle,
                    routeCalculationState = RouteCalculationState.Error(RouteCalculationError.RouteIsTooComplex("")),
                )
            )
        }
        mapViewModel.routeState.update {
            it.copy(
                navigationItem = getDummyNavigationItem(),
            )
        }

        // When
        mapViewModel.setNavigationItem(navigationItem)

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertInstanceOf(RouteCalculationState.NotStarted::class.java, screenState.routeCalculationState)
    }

    @Test
    fun `Given user is planning route and getMissingMapsUseCase fails, when setNavigationItem called, should clear missing maps`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LAT_LON_START_POINT,
            address = "Address",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val endPoint = NavigationPoint(
            latLon = LAT_LON_DESTINATION_POINT,
            address = "",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = endPoint,
        )

        coEvery { getMissingMapsUseCase(navigationItem.getLatLons()) } returns Result.failure(Exception())
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.MissingMapsFound(MISSING_MAPS),
                )
            )
        }
        mapViewModel.routeState.update {
            it.copy(
                navigationItem = mockk(),
                missingMaps = MISSING_MAPS,
            )
        }

        // When
        mapViewModel.setNavigationItem(navigationItem)

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertInstanceOf(MissingMapsState.Idle::class.java, screenState.missingMapsState)
        assertTrue(mapViewModel.routeState.value.missingMaps.isEmpty())
    }

    @Test
    fun `Given user is planning route and start point is null, when setNavigationItem called, should clear missing maps`() {
        // Given
        val endPoint = NavigationPoint(
            latLon = LAT_LON_DESTINATION_POINT,
            address = "",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = null,
            intermediatePoints = emptyList(),
            endPoint = endPoint,
        )

        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.MissingMapsFound(MISSING_MAPS),
                )
            )
        }
        mapViewModel.routeState.update {
            it.copy(
                navigationItem = mockk(),
                missingMaps = MISSING_MAPS,
            )
        }

        // When
        mapViewModel.setNavigationItem(navigationItem)

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertInstanceOf(MissingMapsState.Idle::class.java, screenState.missingMapsState)
        assertTrue(mapViewModel.routeState.value.missingMaps.isEmpty())
    }

    @Test
    fun `Given user is planning route and end point is null, when setNavigationItem called, should clear missing maps`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LAT_LON_START_POINT,
            address = "Address",
            isActive = false,
            isActionActive = true,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = emptyList(),
            endPoint = null,
        )

        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = getDummyNavigationItem(),
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationModeSelection,
                    missingMapsState = MissingMapsState.MissingMapsFound(MISSING_MAPS),
                )
            )
        }
        mapViewModel.routeState.update {
            it.copy(
                navigationItem = mockk(),
                missingMaps = MISSING_MAPS,
            )
        }

        // When
        mapViewModel.setNavigationItem(navigationItem)

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertInstanceOf(MissingMapsState.Idle::class.java, screenState.missingMapsState)
        assertTrue(mapViewModel.routeState.value.missingMaps.isEmpty())
    }

    @Test
    fun `Given a map is indexed, when state is MissingMaps, then missing maps are reloaded`() =
        runTest {
            // Given
            val missingMaps = listOf("a", "b", "c")
            coEvery { getMissingMapsUseCase(any<List<LatLon>>()) } returns Result.success(missingMaps)
            mapViewModel.setNavigationItem(getDummyNavigationItem())
            advanceUntilIdle()
            var screenState = mapViewModel.uiState.value.screenState
            assertEquals(missingMaps, screenState.getMissingMaps())

            // When
            val updatedMissingMaps = listOf("a", "b")
            coEvery { getMissingMapsUseCase(any<List<LatLon>>()) } returns Result.success(updatedMissingMaps)
            onMapsReIndexed.emit(Unit)

            // Then
            screenState = mapViewModel.uiState.value.screenState
            assertEquals(updatedMissingMaps, screenState.getMissingMaps())
    }

    @Test
    fun `Given NavigationInProgress and map moved, when onMapGestureDetected is called, should change isCenterButtonVisible value to true`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = false)
            )
        }

        // When
        mapViewModel.onMapGestureDetected(latLon = LatLon(0.0, 0.0), zoom = 0, moved = true)

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertTrue((screenState as ScreenState.NavigationInProgress).isCenterButtonVisible)
    }

    @Test
    fun `Given StopPoint and map moved, when onMapGestureDetected is called, should change isCenterButtonVisible value to true`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.StopPoint(address = INTERMEDIATE_POINT_ADDRESS, isCenterButtonVisible = false)
            )
        }

        // When
        mapViewModel.onMapGestureDetected(latLon = LatLon(0.0, 0.0), zoom = 0, moved = true)

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertTrue((screenState as ScreenState.StopPoint).isCenterButtonVisible)
    }

    @Test
    fun `Given DestinationReached and map moved, when onMapGestureDetected is called, should change isCenterButtonVisible value to true`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.DestinationReached(address = DESTINATION_POINT_ADDRESS, isCenterButtonVisible = false)
            )
        }

        // When
        mapViewModel.onMapGestureDetected(latLon = LatLon(0.0, 0.0), zoom = 0, moved = true)

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertTrue((screenState as ScreenState.DestinationReached).isCenterButtonVisible)
    }

    @Test
    fun `Given NavigationInProgress, when onCenterNavigationClicked is called, should change isCenterButtonVisible value to true`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.NavigationInProgress(isCenterButtonVisible = true)
            )
        }

        // When
        mapViewModel.onCenterNavigationClicked()

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertFalse((screenState as ScreenState.NavigationInProgress).isCenterButtonVisible)
    }

    @Test
    fun `Given StopPoint, when onCenterNavigationClicked is called, should change isCenterButtonVisible value to true`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.StopPoint(address = INTERMEDIATE_POINT_ADDRESS, isCenterButtonVisible = true)
            )
        }

        // When
        mapViewModel.onCenterNavigationClicked()

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertFalse((screenState as ScreenState.StopPoint).isCenterButtonVisible)
    }

    @Test
    fun `Given DestinationReached, when onCenterNavigationClicked is called, should change isCenterButtonVisible value to true`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.DestinationReached(address = DESTINATION_POINT_ADDRESS, isCenterButtonVisible = true)
            )
        }

        // When
        mapViewModel.onCenterNavigationClicked()

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertFalse((screenState as ScreenState.DestinationReached).isCenterButtonVisible)
    }

    @Test
    fun `Given Idle state, when onUnableToMoveToCurrentLocation is called, should change dialog type to GPS Error`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.Idle(dialogType = ScreenState.Idle.DialogType.None)
            )
        }

        // When
        mapViewModel.onUnableToMoveToCurrentLocation()

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertEquals(ScreenState.Idle.DialogType.GPSError, (screenState as ScreenState.Idle).dialogType)
    }

    @Test
    fun `Given Idle state, when showLocationSharingDialog is called, should change state to idle and show location sharing dialog`() {
        // Given
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.Idle(dialogType = ScreenState.Idle.DialogType.None)
            )
        }

        // When
        mapViewModel.showLocationSharingDialog()

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.Idle
        assertEquals(ScreenState.Idle.DialogType.LocationSharing, screenState.dialogType)
    }

    @Test
    fun `Given SearchResult state, when showLocationSharingDialog is called, should change state to idle and show location sharing dialog`() {
        // Given
        val searchItem = SearchItem(latLon = LAT_LON_START_POINT, itemType = SearchItemType.POI)
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.SearchResult(searchItem = searchItem)
            )
        }
        mapViewModel.routeState.update { it.copy(searchItem = searchItem, myPlaceItem = mockk()) }

        // When
        mapViewModel.showLocationSharingDialog()

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.Idle
        assertEquals(ScreenState.Idle.DialogType.LocationSharing, screenState.dialogType)
        assertNull(mapViewModel.routeState.value.searchItem)
        assertNull(mapViewModel.routeState.value.myPlaceItem)
    }

    @Test
    fun `Given user is planning route, when showLocationSharingDialog is called, should deactivate navigation points and show location sharing dialog`() {
        // Given
        val startPoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = true,
            isActionActive = false,
            type = NavigationPointType.START,
            isCurrentLocation = true,
        )
        val intermediatePoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = false,
            isActionActive = false,
            type = NavigationPointType.INTERMEDIATE,
            isCurrentLocation = false,
        )
        val endPoint = NavigationPoint(
            latLon = LatLon(0.0, 0.0),
            address = "",
            isActive = false,
            isActionActive = false,
            type = NavigationPointType.DESTINATION,
            isCurrentLocation = false,
        )
        val navigationItem = NavigationItem(
            startPoint = startPoint,
            intermediatePoints = listOf(intermediatePoint),
            endPoint = endPoint,
            currentlySelectedPoint = startPoint
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Driving,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        mapViewModel.showLocationSharingDialog()

        // Then
        val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
        assertEquals(RoutePlanningBottomSheetType.LocationSharing, screenState.bottomSheetType)

        assertFalse(screenState.navigationItem.endPoint!!.isActive)
        assertFalse(mapViewModel.routeState.value.navigationItem!!.endPoint!!.isActive)
        assertFalse(screenState.navigationItem.endPoint!!.isActionActive)
        assertFalse(mapViewModel.routeState.value.navigationItem!!.endPoint!!.isActionActive)

        assertFalse(screenState.navigationItem.intermediatePoints.first().isActive)
        assertFalse(mapViewModel.routeState.value.navigationItem!!.intermediatePoints.first().isActive)
        assertFalse(screenState.navigationItem.intermediatePoints.first().isActionActive)
        assertFalse(mapViewModel.routeState.value.navigationItem!!.intermediatePoints.first().isActionActive)

        assertNotNull(screenState.navigationItem.startPoint)
        assertNotNull(mapViewModel.routeState.value.navigationItem!!.startPoint)

        assertNull(screenState.navigationItem.currentlySelectedPoint)
        assertNull(mapViewModel.routeState.value.navigationItem!!.currentlySelectedPoint)
    }

    @Test
    fun `Given location sharing request in planning route state, when closeLocationSharingDialog called, then should activate navigation points`() {
        runTest {
            // Given
            val start = NavigationPoint(latLon = LAT_LON_START_POINT, address = "", isActive = false, isActionActive = false)
            val end = NavigationPoint(latLon = LAT_LON_DESTINATION_POINT, address = "", isActive = false, isActionActive = false)
            val navigationItem = NavigationItem(
                startPoint = start,
                intermediatePoints = emptyList(),
                endPoint = end,
                currentlySelectedPoint = null
            )

            mapViewModel.routeState.update {
                it.copy(navigationItem = navigationItem)
            }
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = navigationItem,
                        selectedNavigationMode = NavigationModeItem.Driving,
                        bottomSheetType = RoutePlanningBottomSheetType.LocationSharing,
                        missingMapsState = MissingMapsState.Idle,
                    )
                )
            }

            // When
            mapViewModel.closeLocationSharingDialog()

            // Then
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            val expectedNavigationItem = navigationItem.copy(
                startPoint = start.copy(isActive = true, isActionActive = true),
                endPoint = end.copy(isActive = true, isActionActive = true),
            )
            assertEquals(expectedNavigationItem, mapViewModel.routeState.value.navigationItem)
            assertEquals(expectedNavigationItem, screenState.navigationItem)
            assertEquals(RoutePlanningBottomSheetType.NavigationModeSelection, screenState.bottomSheetType)
        }
    }

    @Test
    fun `Given location sharing request in idle state, when closeLocationSharingDialog called, then should update state`() {
        runTest {
            // Given
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.Idle(
                        dialogType = ScreenState.Idle.DialogType.LocationSharing
                    )
                )
            }

            // When
            mapViewModel.closeLocationSharingDialog()

            // Then
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.Idle
            assertEquals(ScreenState.Idle.DialogType.None, screenState.dialogType)
        }
    }

    @Test
    fun `Given map is displayed, when the map center points to a missing region, then a missing region overlay is displayed`() {
        runTest {
            // Given
            val missingRegion = MISSING_MAPS.first()
            coEvery { getMissingMapsUseCase(latLon = any()) } returns Result.success(listOf(missingRegion))
            val detectTurbine = mapViewModel.detectMissingRegions().testIn(this)
            val uiStateTurbine = mapViewModel.uiState.testIn(this)

            // When
            mapViewModel.checkMissingRegion(LatLon(0.0, 0.0))
            detectTurbine.awaitItem()

            // Then
            assertEquals(
                ScreenState.MissingRegionOverlay(missingRegion),
                uiStateTurbine.expectMostRecentItem().screenState,
            )
            uiStateTurbine.cancel()
            detectTurbine.cancel()
        }
    }

    @Test
    fun `Given map is displayed, when the map center points to a downloaded region, then screen state remains the same`() {
        runTest {
            // Given
            coEvery { getMissingMapsUseCase(latLon = any()) } returns Result.success(emptyList())
            val detectTurbine = mapViewModel.detectMissingRegions().testIn(this)
            val uiStateTurbine = mapViewModel.uiState.testIn(this)
            uiStateTurbine.awaitItem()

            // When
            mapViewModel.checkMissingRegion(LatLon(0.0, 0.0))
            detectTurbine.awaitItem()

            // Then
            uiStateTurbine.expectNoEvents()

            uiStateTurbine.cancel()
            detectTurbine.cancel()
        }
    }

    @Test
    fun `Given missing region is displayed, when maps are re-indexed with the missing region, then a missing region overlay is hidden`() {
        runTest {
            // Given
            val missingRegion = MISSING_MAPS.first()
            coEvery { getMissingMapsUseCase(latLon = any()) } returns Result.success(listOf(missingRegion))
            val detectTurbine = mapViewModel.detectMissingRegions().testIn(this)
            val uiStateTurbine = mapViewModel.uiState.testIn(this)

            // When
            mapViewModel.checkMissingRegion(LatLon(0.0, 0.0))
            detectTurbine.awaitItem()
            assertEquals(
                ScreenState.MissingRegionOverlay(missingRegion),
                uiStateTurbine.expectMostRecentItem().screenState,
            )
            coEvery { getMissingMapsUseCase(latLon = any()) } returns Result.success(emptyList())
            onMapsReIndexed.emit(Unit)

            // Then
            detectTurbine.awaitItem()
            assertEquals(ScreenState.Idle(), uiStateTurbine.expectMostRecentItem().screenState)

            uiStateTurbine.cancel()
            detectTurbine.cancel()
        }
    }

    @Test
    fun `Given user is viewing a map, when map gesture is detected, then the map last known location is saved`() {
        runTest {
            // Given
            val latLon = LatLon(55.0, 50.0)
            val zoom  = 14

            val deferred = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                 mapViewModel.observeAndSaveMapLocationFlow().first()
            }

            // When
            mapViewModel.onMapGestureDetected(latLon = latLon, zoom = zoom, moved = false)
            deferred.await()

            // Then
            coVerify(exactly = 1) { setMapLastKnownLocationUseCase(latLon, zoom) }
        }
    }

    @Test
    fun `Given map is displayed, when the initial screen state loads, then the map rotation is disabled`() {
        runTest {
            // Given
            val uiState = mapViewModel.uiState.testIn(this)

            // When
            uiState.expectMostRecentItem()

            // Then
            coVerify(exactly = 1) { changeMapRotationModeUseCase.invoke(false, any()) }

            uiState.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given map is displayed, when the navigation starts, then the map rotation is enabled`() {
        runTest {
            // Given
            val uiState = mapViewModel.uiState.testIn(this)
            every { osmAndFormatter.getFormattedDistanceValue(0f) } returns OsmAndFormatter.FormattedValue("0", "m", false)

            // When
            mapViewModel.prepareForNavigation(getDummyNavigationItem(), null)
            mapViewModel.onRouteLoading()
            mapViewModel.updateNavigationProperties(
                estimatedRouteDistance = 0,
                estimatedRouteTime = 0,
                estimatedNextTurnDistance = 0,
                routeDirections = listOf(
                    RouteDirectionInfo(averageSpeed = 50f, turnType = TurnType.straight()),
                ),
            ) { EMPTY_LOCATION }
            uiState.expectMostRecentItem()

            // Then
            coVerify { changeMapRotationModeUseCase.invoke(true, any()) }

            uiState.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given start and destination points, when activateNavigationItemPoints called, then should activate points and their actions and set correct screen state`() {
        runTest {
            // Given
            val start = NavigationPoint(latLon = LAT_LON_START_POINT, address = "", isActive = true, isActionActive = false)
            val end = NavigationPoint(latLon = LAT_LON_DESTINATION_POINT, address = "", isActive = false, isActionActive = false)
            val navigationItem = NavigationItem(
                startPoint = start,
                intermediatePoints = emptyList(),
                endPoint = end,
                currentlySelectedPoint = start
            )
            mapViewModel.routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                    missingMaps = emptyList()
                )
            }

            // When
            mapViewModel.activateNavigationItemPoints {  }

            // Then
            val expected = navigationItem.copy(
                startPoint = start.copy(isActionActive = true),
                endPoint = end.copy(isActive = true, isActionActive = true),
                currentlySelectedPoint = null,
            )
            assertEquals(expected, mapViewModel.routeState.value.navigationItem)
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            assertEquals(expected, screenState.navigationItem)
            assertEquals(RoutePlanningBottomSheetType.NavigationModeSelection, screenState.bottomSheetType)
            assertEquals(MissingMapsState.Idle, screenState.missingMapsState)
        }
    }

    @Test
    fun `Given start point only, when activateNavigationItemPoints called, then should activate start point but its action should stay disabled and set correct screen state`() {
        runTest {
            // Given
            val start = NavigationPoint(latLon = LAT_LON_START_POINT, address = "", isActive = false, isActionActive = false)
            val navigationItem = NavigationItem(
                startPoint = start,
                intermediatePoints = emptyList(),
                endPoint = null,
            )
            mapViewModel.routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                    missingMaps = emptyList()
                )
            }

            // When
            mapViewModel.activateNavigationItemPoints {  }

            // Then
            val expected = navigationItem.copy(
                startPoint = start.copy(isActive = true),
            )
            assertEquals(expected, mapViewModel.routeState.value.navigationItem)
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            assertEquals(expected, screenState.navigationItem)
            assertEquals(RoutePlanningBottomSheetType.NavigationModeSelection, screenState.bottomSheetType)
            assertEquals(MissingMapsState.Idle, screenState.missingMapsState)
        }
    }

    @Test
    fun `Given start, destination and max intermediate points, when activateNavigationItemPoints called, then should activate all points, activate start and intermediate points actions and set correct screen state`() {
        runTest {
            // Given
            val start = NavigationPoint(latLon = LAT_LON_START_POINT, address = "", isActive = false, isActionActive = false)
            val end = NavigationPoint(latLon = LAT_LON_DESTINATION_POINT, address = "", isActive = false, isActionActive = false)
            val firstIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = false, isActionActive = false)
            val secondIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = false, isActionActive = false)
            val thirdIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true)
            val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
            val navigationItem = NavigationItem(
                startPoint = start,
                intermediatePoints = intermediate,
                endPoint = end,
                currentlySelectedPoint = thirdIntermediate
            )
            mapViewModel.routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                    missingMaps = emptyList()
                )
            }

            // When
            mapViewModel.activateNavigationItemPoints {  }

            // Then
            val expected = navigationItem.copy(
                startPoint = start.copy(isActive = true, isActionActive = true),
                intermediatePoints = listOf(
                    firstIntermediate.copy(isActive = true, isActionActive = true),
                    secondIntermediate.copy(isActive = true, isActionActive = true),
                    thirdIntermediate.copy(isActive = true, isActionActive = true)
                ),
                endPoint = end.copy(isActive = true, isActionActive = false),
                currentlySelectedPoint = null,
            )
            assertEquals(expected, mapViewModel.routeState.value.navigationItem)
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            assertEquals(expected, screenState.navigationItem)
            assertEquals(RoutePlanningBottomSheetType.NavigationModeSelection, screenState.bottomSheetType)
            assertEquals(MissingMapsState.Idle, screenState.missingMapsState)
        }
    }

    @Test
    fun `Given navigation item and some missing maps, when activateNavigationItemPoints called, then should update screen state with missing maps`() {
        runTest {
            // Given
            val navigationItem = getDummyNavigationItem()
            val missingMaps = listOf("Croatia_europe", "Germany_europe")
            mapViewModel.routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                    missingMaps = missingMaps,
                )
            }

            // When
            mapViewModel.activateNavigationItemPoints {  }

            // Then
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            assertEquals(MissingMapsState.MissingMapsFound(missingMaps), screenState.missingMapsState)
        }
    }

    @Test
    fun `Given MissingRegionOverlay screen state, when dismissMissingRegion called, then should change state to Idle`() {
        runTest {
            // Given
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.MissingRegionOverlay("Croatia_europe")
                )
            }

            // When
            mapViewModel.dismissMissingRegion()

            // Then
            assertInstanceOf(ScreenState.Idle::class.java, mapViewModel.uiState.value.screenState)
        }
    }

    @Test
    fun `Given navigation item, when selectNavigationPoint called, then should activate navigation points and update screen state to SelectLocation`() {
        runTest {
            // Given
            val start = NavigationPoint(latLon = LAT_LON_START_POINT, address = "", isActive = true, isActionActive = false)
            val end = NavigationPoint(latLon = LAT_LON_DESTINATION_POINT, address = "", isActive = false, isActionActive = false)
            val navigationItem = NavigationItem(
                startPoint = start,
                intermediatePoints = emptyList(),
                endPoint = end,
                currentlySelectedPoint = start
            )
            mapViewModel.routeState.update {
                it.copy(navigationItem = navigationItem)
            }

            // When
            mapViewModel.selectNavigationPoint(navigationItem)

            // Then
            val screenState = mapViewModel.uiState.value.screenState
            assertInstanceOf(ScreenState.SelectLocation::class.java, screenState)
            val expectedNavigationItem = navigationItem.copy(
                startPoint = start.copy(isActive = true, isActionActive = true),
                endPoint = end.copy(isActive = true, isActionActive = true),
            )
            assertEquals(expectedNavigationItem, (screenState as ScreenState.SelectLocation).navigationItem)
        }
    }

    @Test
    fun `Given start point selected, when onNavigationPointSelected called, then deactivate other points`() {
        runTest {
            // Given
            val start = NavigationPoint(latLon = LAT_LON_START_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.START)
            val end = NavigationPoint(latLon = LAT_LON_DESTINATION_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.DESTINATION)
            val firstIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val secondIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val thirdIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
            val navigationItem = NavigationItem(
                startPoint = start,
                intermediatePoints = intermediate,
                endPoint = end,
                currentlySelectedPoint = null
            )

            mapViewModel.routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                )
            }
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = navigationItem,
                        selectedNavigationMode = NavigationModeItem.Driving,
                        bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                        missingMapsState = MissingMapsState.Idle,
                    )
                )
            }

            // When
            val navPointItem = NavigationPointItem.Start(
                startAddress = "",
                startLocation = null,
                isCurrentLocation = false,
                isActive = true,
                isActionButtonActive = true,
                type = NavigationPointType.START,
            )
            mapViewModel.onNavigationPointSelected(navPointItem)

            // Then
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            val expectedNavigationItem = navigationItem.copy(
                startPoint = start.copy(isActive = true, isActionActive = false),
                intermediatePoints = listOf(
                    firstIntermediate.copy(isActive = false, isActionActive = false),
                    secondIntermediate.copy(isActive = false, isActionActive = false),
                    thirdIntermediate.copy(isActive = false, isActionActive = false)
                ),
                endPoint = end.copy(isActive = false, isActionActive = false),
                currentlySelectedPoint = start,
            )
            assertEquals(expectedNavigationItem, mapViewModel.routeState.value.navigationItem)
            assertEquals(expectedNavigationItem, screenState.navigationItem)
        }
    }

    @Test
    fun `Given end point selected, when onNavigationPointSelected called, then deactivate other points`() {
        runTest {
            // Given
            val start = NavigationPoint(latLon = LAT_LON_START_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.START)
            val end = NavigationPoint(latLon = LAT_LON_DESTINATION_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.DESTINATION)
            val firstIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val secondIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val thirdIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
            val navigationItem = NavigationItem(
                startPoint = start,
                intermediatePoints = intermediate,
                endPoint = end,
                currentlySelectedPoint = null
            )

            mapViewModel.routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                )
            }
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = navigationItem,
                        selectedNavigationMode = NavigationModeItem.Driving,
                        bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                        missingMapsState = MissingMapsState.Idle,
                    )
                )
            }

            // When
            val navPointItem = NavigationPointItem.Destination(
                finishAddress = "",
                finishLocation = null,
                isActive = true,
                isActionButtonActive = true,
                type = NavigationPointType.DESTINATION,
            )
            mapViewModel.onNavigationPointSelected(navPointItem)

            // Then
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            val expectedNavigationItem = navigationItem.copy(
                startPoint = start.copy(isActive = false, isActionActive = false),
                intermediatePoints = listOf(
                    firstIntermediate.copy(isActive = false, isActionActive = false),
                    secondIntermediate.copy(isActive = false, isActionActive = false),
                    thirdIntermediate.copy(isActive = false, isActionActive = false)
                ),
                endPoint = end.copy(isActive = true, isActionActive = false),
                currentlySelectedPoint = end,
            )
            assertEquals(expectedNavigationItem, mapViewModel.routeState.value.navigationItem)
            assertEquals(expectedNavigationItem, screenState.navigationItem)
        }
    }

    @Test
    fun `Given intermediate point selected, when onNavigationPointSelected called, then deactivate other points`() {
        runTest {
            // Given
            val start = NavigationPoint(latLon = LAT_LON_START_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.START)
            val end = NavigationPoint(latLon = LAT_LON_DESTINATION_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.DESTINATION)
            val firstIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val secondIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val thirdIntermediate = NavigationPoint(latLon = LAT_LON_INTERMEDIATE_POINT, address = "", isActive = true, isActionActive = true, type = NavigationPointType.INTERMEDIATE)
            val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
            val navigationItem = NavigationItem(
                startPoint = start,
                intermediatePoints = intermediate,
                endPoint = end,
                currentlySelectedPoint = null
            )

            mapViewModel.routeState.update {
                it.copy(
                    navigationItem = navigationItem,
                )
            }
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.PlanningRoute(
                        navigationItem = navigationItem,
                        selectedNavigationMode = NavigationModeItem.Driving,
                        bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                        missingMapsState = MissingMapsState.Idle,
                    )
                )
            }

            // When
            val navPointItem = NavigationPointItem.Intermediate(
                uuid = secondIntermediate.uuid,
                intermediateAddress = "",
                intermediateLocation = null,
                isActive = true,
                isActionButtonActive = true,
                type = NavigationPointType.INTERMEDIATE
            )
            mapViewModel.onNavigationPointSelected(navPointItem)

            // Then
            val screenState = mapViewModel.uiState.value.screenState as ScreenState.PlanningRoute
            val expectedNavigationItem = navigationItem.copy(
                startPoint = start.copy(isActive = false, isActionActive = false),
                intermediatePoints = listOf(
                    firstIntermediate.copy(isActive = false, isActionActive = false),
                    secondIntermediate.copy(isActive = true, isActionActive = false),
                    thirdIntermediate.copy(isActive = false, isActionActive = false)
                ),
                endPoint = end.copy(isActive = false, isActionActive = false),
                currentlySelectedPoint = secondIntermediate,
            )
            assertEquals(expectedNavigationItem, mapViewModel.routeState.value.navigationItem)
            assertEquals(expectedNavigationItem, screenState.navigationItem)
        }
    }

    @Test
    fun `Given Idle state, when onLongPressed called, then should search address of selected coordinates`() {
        runTest {
            // Given
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.Idle()
                )
            }

            val address = GeocodingAddress(
                city = "Warszawa",
                street = "aleja Zieleniecka",
                buildingNumber = "1",
                postcode = "03-727",
                latLon = LAT_LON_DESTINATION_POINT
            )
            coEvery { geocodingRepository.searchAddress(any()) } returns Result.success(address)
            coEvery { myPlacesRepository.getMyPlaces() } returns Result.success(emptyList())

            // When
            mapViewModel.onLongPressed(LAT_LON_DESTINATION_POINT)

            // Then
            val screenState = mapViewModel.uiState.value.screenState
            assertInstanceOf(ScreenState.SearchResult::class.java, mapViewModel.uiState.value.screenState)
            assertEquals((screenState as ScreenState.SearchResult).searchItem.localName, "${address.street} ${address.buildingNumber}")
            assertEquals(screenState.searchItem.desc, address.city)
            assertNull(mapViewModel.routeState.value.navigationItem)
            assertNotNull(mapViewModel.routeState.value.searchItem)
        }
    }

    @Test
    fun `Given SearchResult state, when onLongPressed called, then should search address of selected coordinates`() {
        runTest {
            // Given
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.SearchResult(mockk())
                )
            }

            val address = GeocodingAddress(
                city = "Warszawa",
                street = "aleja Zieleniecka",
                buildingNumber = "1",
                postcode = "03-727",
                latLon = LAT_LON_DESTINATION_POINT
            )
            coEvery { geocodingRepository.searchAddress(any()) } returns Result.success(address)
            coEvery { myPlacesRepository.getMyPlaces() } returns Result.success(emptyList())

            // When
            mapViewModel.onLongPressed(LAT_LON_DESTINATION_POINT)

            // Then
            val screenState = mapViewModel.uiState.value.screenState
            assertInstanceOf(ScreenState.SearchResult::class.java, mapViewModel.uiState.value.screenState)
            assertEquals((screenState as ScreenState.SearchResult).searchItem.localName, "${address.street} ${address.buildingNumber}")
            assertEquals(screenState.searchItem.desc, address.city)
            assertNull(mapViewModel.routeState.value.navigationItem)
            assertNotNull(mapViewModel.routeState.value.searchItem)
        }
    }

    @Test
    fun `Given MissingRegionOverlay state, when onLongPressed called, then should search address of selected coordinates`() {
        runTest {
            // Given
            mapViewModel.uiState.update {
                it.copy(
                    screenState = ScreenState.MissingRegionOverlay("")
                )
            }

            val address = GeocodingAddress(
                city = "Warszawa",
                street = "aleja Zieleniecka",
                buildingNumber = "1",
                postcode = "03-727",
                latLon = LAT_LON_DESTINATION_POINT
            )
            coEvery { geocodingRepository.searchAddress(any()) } returns Result.success(address)
            coEvery { myPlacesRepository.getMyPlaces() } returns Result.success(emptyList())

            // When
            mapViewModel.onLongPressed(LAT_LON_DESTINATION_POINT)

            // Then
            val screenState = mapViewModel.uiState.value.screenState
            assertInstanceOf(ScreenState.SearchResult::class.java, mapViewModel.uiState.value.screenState)
            assertEquals((screenState as ScreenState.SearchResult).searchItem.localName, "${address.street} ${address.buildingNumber}")
            assertEquals(screenState.searchItem.desc, address.city)
            assertNull(mapViewModel.routeState.value.navigationItem)
            assertNotNull(mapViewModel.routeState.value.searchItem)
        }
    }

    @Test
    fun `Given cycling mode and 3 intermediate points and distance between last two points exceeds limit, when checkIfRouteIsTooLong is called, should return true`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39296, 16.91702), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(51.89017, 16.03754), address = "", isActive = true, isActionActive = false)
        val firstIntermediate = NavigationPoint(latLon = LatLon(52.22484, 18.25486), address = "", isActive = true, isActionActive = true)
        val secondIntermediate = NavigationPoint(latLon = LatLon(52.48498, 17.27994), address = "", isActive = true, isActionActive = true)
        val thirdIntermediate = NavigationPoint(latLon = LatLon(51.64224, 17.83591), address = "", isActive = true, isActionActive = true)
        val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = intermediate,
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Cycling,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given cycling mode and 3 intermediate points and distance limit between points is not exceeded, when checkIfRouteIsTooLong is called, should return false`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39296, 16.91702), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(51.66085, 16.07461), address = "", isActive = true, isActionActive = false)
        val firstIntermediate = NavigationPoint(latLon = LatLon(52.22484, 18.25486), address = "", isActive = true, isActionActive = true)
        val secondIntermediate = NavigationPoint(latLon = LatLon(52.48498, 17.27994), address = "", isActive = true, isActionActive = true)
        val thirdIntermediate = NavigationPoint(latLon = LatLon(51.84721, 16.59801), address = "", isActive = true, isActionActive = true)
        val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = intermediate,
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Cycling,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertFalse(result)
    }

    @Test
    fun `Given cycling mode and distance between start and destination exceeds limit, when checkIfRouteIsTooLong is called, should return true`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39296, 16.91702), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(51.40595, 16.22562), address = "", isActive = true, isActionActive = false)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = emptyList(),
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Cycling,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given cycling mode and distance between start and destination does not limit, when checkIfRouteIsTooLong is called, should return false`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39296, 16.91702), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(51.50882, 16.26664), address = "", isActive = true, isActionActive = false)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = emptyList(),
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Cycling,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertFalse(result)
    }

    @Test
    fun `Given walking mode and 3 intermediate points and distance between last two points exceeds limit, when checkIfRouteIsTooLong is called, should return true`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39310, 16.92505), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(52.06650, 16.01531), address = "", isActive = true, isActionActive = false)
        val firstIntermediate = NavigationPoint(latLon = LatLon(52.22383, 16.44934), address = "", isActive = true, isActionActive = true)
        val secondIntermediate = NavigationPoint(latLon = LatLon(52.09635, 16.96167), address = "", isActive = true, isActionActive = true)
        val thirdIntermediate = NavigationPoint(latLon = LatLon(51.87068, 16.57437), address = "", isActive = true, isActionActive = true)
        val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = intermediate,
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given walking mode and 3 intermediate points and distance limit between points is not exceeded, when checkIfRouteIsTooLong is called, should return false`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39310, 16.92505), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(51.90812, 16.20455), address = "", isActive = true, isActionActive = false)
        val firstIntermediate = NavigationPoint(latLon = LatLon(52.22383, 16.44934), address = "", isActive = true, isActionActive = true)
        val secondIntermediate = NavigationPoint(latLon = LatLon(52.09635, 16.96167), address = "", isActive = true, isActionActive = true)
        val thirdIntermediate = NavigationPoint(latLon = LatLon(51.87068, 16.57437), address = "", isActive = true, isActionActive = true)
        val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = intermediate,
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertFalse(result)
    }

    @Test
    fun `Given walking mode and 3 intermediate points and distance limit between points is not exceeded but route distance limit is exceeded, when checkIfRouteIsTooLong is called, should return true`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39310, 16.92505), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(51.98956, 16.08424), address = "", isActive = true, isActionActive = false)
        val firstIntermediate = NavigationPoint(latLon = LatLon(52.22383, 16.44934), address = "", isActive = true, isActionActive = true)
        val secondIntermediate = NavigationPoint(latLon = LatLon(52.09635, 16.96167), address = "", isActive = true, isActionActive = true)
        val thirdIntermediate = NavigationPoint(latLon = LatLon(51.87068, 16.57437), address = "", isActive = true, isActionActive = true)
        val intermediate = listOf(firstIntermediate, secondIntermediate, thirdIntermediate)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = intermediate,
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given walking mode and distance between start and destination exceeds limit, when checkIfRouteIsTooLong is called, should return true`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39310, 16.92505), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(52.05915, 16.60446), address = "", isActive = true, isActionActive = false)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = emptyList(),
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given walking mode and distance between start and destination does not limit, when checkIfRouteIsTooLong is called, should return false`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39310, 16.92505), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(52.08031, 16.71546), address = "", isActive = true, isActionActive = false)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = emptyList(),
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertFalse(result)
    }

    @Test
    fun `Given startPoint is null, when checkIfRouteIsTooLong is called, should return false`() {
        // Given
        val end = NavigationPoint(latLon = LatLon(52.05915, 16.60446), address = "", isActive = true, isActionActive = false)
        val navigationItem = NavigationItem(
            startPoint = null,
            intermediatePoints = emptyList(),
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertFalse(result)
    }

    @Test
    fun `Given endPoint is null, when checkIfRouteIsTooLong is called, should return false`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.05915, 16.60446), address = "", isActive = true, isActionActive = false)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = emptyList(),
            endPoint = null,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        val result = mapViewModel.checkIfRouteIsTooLong(navigationItem)

        // Then
        assertFalse(result)
    }

    @Test
    fun `Given route calculation error, when onDismissRouteCalculationError is called, should clear the error`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39310, 16.92505), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(52.08031, 16.71546), address = "", isActive = true, isActionActive = false)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = emptyList(),
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                    routeCalculationState = RouteCalculationState.Error(RouteCalculationError.RouteIsTooComplex(""))
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        mapViewModel.onDismissRouteCalculationError()

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertInstanceOf(RouteCalculationState.NotStarted::class.java, (screenState as ScreenState.PlanningRoute).routeCalculationState)
    }

    @Test
    fun `Given long route calculation alert, when onContinueRouteCalculationClick is called, should change calculation stage to Continued`() {
        // Given
        val start = NavigationPoint(latLon = LatLon(52.39310, 16.92505), address = "", isActive = true, isActionActive = false)
        val end = NavigationPoint(latLon = LatLon(52.08031, 16.71546), address = "", isActive = true, isActionActive = false)
        val navigationItem = NavigationItem(
            startPoint = start,
            intermediatePoints = emptyList(),
            endPoint = end,
            currentlySelectedPoint = null
        )
        mapViewModel.uiState.update {
            it.copy(
                screenState = ScreenState.PlanningRoute(
                    navigationItem = navigationItem,
                    selectedNavigationMode = NavigationModeItem.Walking,
                    bottomSheetType = RoutePlanningBottomSheetType.NavigationPointSelection,
                    missingMapsState = MissingMapsState.Idle,
                    routeCalculationState = RouteCalculationState.InProgress(RouteCalculationState.InProgress.Stage.Alert)
                )
            )
        }
        mapViewModel.routeState.update { it.copy(navigationItem = navigationItem) }

        // When
        mapViewModel.onContinueRouteCalculationClick()

        // Then
        val screenState = mapViewModel.uiState.value.screenState
        assertInstanceOf(RouteCalculationState.InProgress::class.java, (screenState as ScreenState.PlanningRoute).routeCalculationState)
        assertEquals(RouteCalculationState.InProgress.Stage.Continued, (screenState.routeCalculationState as RouteCalculationState.InProgress).stage)
    }

    @Test
    fun `Given no start location, when checking if alert should be shown, then the check returns true only once`() {
        val navigationItemWithoutStartLocation = getDummyNavigationItem().copy(startPoint = null)
        assertTrue(mapViewModel.shouldShowInitialNoLocationError(navigationItemWithoutStartLocation))
        assertFalse(mapViewModel.shouldShowInitialNoLocationError(navigationItemWithoutStartLocation))
    }

    @Test
    fun `Given planning route and the NavigationItem has no start location, when initial location is obtained, then the NavigationItem is updated`() {
        // Given
        val navigationItemWithoutStartLocation = getDummyNavigationItem().copy(startPoint = null)
        mapViewModel.setNavigationItem(navigationItemWithoutStartLocation)
        var planRoute = mapViewModel.uiState.value.screenState
        assertNull((planRoute as ScreenState.PlanningRoute).navigationItem.startPoint)
        val startLatLon = getDummyLatLon()

        // When
        mapViewModel.onInitialLocation(startLatLon)

        //Then
        planRoute = mapViewModel.uiState.value.screenState
        assertEquals(startLatLon, (planRoute as ScreenState.PlanningRoute).navigationItem.startPoint?.latLon)
    }

    private fun ScreenState.getMissingMaps(): List<String> =
        ((this as ScreenState.PlanningRoute)
            .missingMapsState as MissingMapsState.MissingMapsFound).missingMaps

    private fun getDummyNavigationItem(): NavigationItem = NavigationItem(
        startPoint = NavigationPoint(LAT_LON_START_POINT, ""),
        intermediatePoints = emptyList(),
        endPoint = NavigationPoint(LAT_LON_DESTINATION_POINT, ""),
    )

    private fun getDummyLatLon(): LatLon = LatLon(0.0, 0.0)

    companion object {
        private const val START_POINT_ADDRESS: String = "Cicha 25"
        private const val INTERMEDIATE_POINT_ADDRESS: String = "Kolorowa 64"
        private const val DESTINATION_POINT_ADDRESS: String = "Kolejowa 6"
        private const val CURRENT_POINT_ADDRESS = "Current location"
        private val CURRENT_POINT_LATLON = LatLon(99.0, 99.0)

        private const val START_DESTINATION_DISTANCE_METERS: Double = 130000.0
        private const val START_DESTINATION_DISTANCE: String = "130km"

        private const val NEXT_TURN_DISTANCE_METERS: Double = 3000.0
        private const val NEXT_TURN_DISTANCE: String = "3km"

        private val LAT_LON_START_POINT: LatLon = LatLon(51.772592, 19.476798)
        private val LAT_LON_INTERMEDIATE_POINT: LatLon = LatLon(52.106525, 19.948460)
        private val LAT_LON_DESTINATION_POINT: LatLon = LatLon(52.240654, 21.050564)
        private val EMPTY_LOCATION = Location("", 0.0, 0.0)

        private val MISSING_MAPS: List<String> = listOf("Poland_wielkopolskie_europe.zip", "Poland_lubuskie_europe.zip")
    }
}