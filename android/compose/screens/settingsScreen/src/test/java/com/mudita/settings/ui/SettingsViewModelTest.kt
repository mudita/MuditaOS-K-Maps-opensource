package com.mudita.settings.ui

import com.mudita.map.common.enums.MetricsConstants
import com.mudita.map.common.model.SettingItem
import com.mudita.map.common.model.SettingItemAction
import com.mudita.map.common.model.SettingsData
import com.mudita.map.common.model.StorageType
import com.mudita.map.common.repository.SettingsRepository
import com.mudita.map.common.sharedPrefs.SDCardPreferencesManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class SettingsViewModelTest {

    @RelaxedMockK private lateinit var settingsRepository: SettingsRepository
    @RelaxedMockK private lateinit var sdCardPreferencesManager: SDCardPreferencesManager

    private lateinit var viewModel: SettingsViewModel

    private val testCoroutineScheduler = TestCoroutineScheduler()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testCoroutineScheduler))
        viewModel = SettingsViewModel(
            settingsRepository,
            sdCardPreferencesManager,
        )
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given SettingsData, when initSettings called, should update state and sd card manager`() {
        // Given
        val settingsData = SettingsData(
            soundEnabled = true,
            screenAlwaysOn = true,
            wifiOnlyEnabled = true,
            sdCardExists = true,
            storageType = StorageType.PHONE,
            distanceUnits = MetricsConstants.KILOMETERS_AND_METERS
        )

        // When
        viewModel.initSettings(settingsData)

        // Then
        assertEquals(settingsData, viewModel.state.value.settingsData)
        coVerify { sdCardPreferencesManager.updateSDCardEnabled(true) }
    }

    @Test
    fun `Given SettingsData, when refreshState called, should update state with settings items`() {
        // Given
        val settingsData = SettingsData(
            soundEnabled = true,
            screenAlwaysOn = true,
            wifiOnlyEnabled = true,
            sdCardExists = true,
            storageType = StorageType.PHONE,
            distanceUnits = MetricsConstants.KILOMETERS_AND_METERS
        )
        val settingsItems = listOf(
            SettingItem.ScreenAlwaysOn(title = 1, isChecked = true),
            SettingItem.WifiOnly(title = 2, isChecked = true),
            SettingItem.Storage(
                title = 3,
                options = listOf(
                    SettingItemAction.Checkable.CheckableItem.Card(isAvailable = true, isChecked = false),
                    SettingItemAction.Checkable.CheckableItem.Phone(isChecked = true)
                )
            ),
            SettingItem.DistanceUnits(
                title = 4,
                options = listOf(
                    SettingItemAction.Checkable.CheckableItem.Miles(isChecked = false),
                    SettingItemAction.Checkable.CheckableItem.Kilometers(isChecked = true)
                )
            )
        )

        viewModel.state.update { it.copy(settingsData = settingsData) }
        every { settingsRepository.getSettingsItems(any()) } returns settingsItems

        // When
        viewModel.refreshState()

        // Then
        assertEquals(settingsItems, viewModel.state.value.settingItems)
    }

    @Test
    fun `When screen always on setting switched, then should save value in repository`() {
        // Given
        val settingItem = SettingItem.ScreenAlwaysOn(title = 1, isChecked = true)

        // When
        viewModel.onItemSwitched(settingItem)

        // Then
        verify { settingsRepository.saveScreenAlwaysOnEnabled(true) }
    }

    @Test
    fun `When sound setting switched, then should save value in repository`() {
        // Given
        val settingItem = SettingItem.Sound(title = 1, isChecked = true)

        // When
        viewModel.onItemSwitched(settingItem)

        // Then
        verify { settingsRepository.saveSoundEnabled(false) } // isMuted = soundEnabled.not()
    }

    @Test
    fun `When wifi only setting switched, then should save value in repository`() {
        // Given
        val settingItem = SettingItem.WifiOnly(title = 1, isChecked = true)

        // When
        viewModel.onItemSwitched(settingItem)

        // Then
        verify { settingsRepository.saveWifiOnlyEnabled(true) }
    }

    @Test
    fun `When sd card storage option selected, then should update state`() {
        // Given
        val option = SettingItemAction.Checkable.CheckableItem.Card(isAvailable = true, isChecked = true)
        viewModel.state.update { it.copy(settingsData = SettingsData(sdCardExists = true, storageType = StorageType.PHONE)) }

        // When
        viewModel.onItemChecked(option)

        // Then
        assertEquals(StorageType.SD_CARD, viewModel.state.value.settingsData.storageType)
    }

    @Test
    fun `When phone storage option selected, then should update state`() {
        // Given
        val option = SettingItemAction.Checkable.CheckableItem.Phone(isChecked = true)
        viewModel.state.update { it.copy(settingsData = SettingsData(sdCardExists = true, storageType = StorageType.SD_CARD)) }

        // When
        viewModel.onItemChecked(option)

        // Then
        assertEquals(StorageType.PHONE, viewModel.state.value.settingsData.storageType)
    }

    @Test
    fun `When kilometers distance unit option selected, then should update state and save value in repository`() {
        // Given
        val milesOption = SettingItemAction.Checkable.CheckableItem.Miles(isChecked = true)
        val kilometersOption = SettingItemAction.Checkable.CheckableItem.Kilometers(isChecked = false)
        val settingsItems = listOf(
            SettingItem.DistanceUnits(
                title = 4,
                options = listOf(milesOption, kilometersOption)
            )
        )

        viewModel.state.update { it.copy(settingsData = SettingsData(distanceUnits = MetricsConstants.MILES_AND_FEET), settingItems = settingsItems) }

        // When
        viewModel.onItemChecked(kilometersOption)

        // Then
        val actualDistanceUnitSettingItem = (viewModel.state.value.settingItems.first() as SettingItem.DistanceUnits)
        assertFalse(actualDistanceUnitSettingItem.options.first().isChecked)
        assertTrue(actualDistanceUnitSettingItem.options.last().isChecked)
        verify { settingsRepository.saveMetricUnits(MetricsConstants.KILOMETERS_AND_METERS) }
    }

    @Test
    fun `When miles distance unit option selected, then should update state and save value in repository`() {
        // Given
        val milesOption = SettingItemAction.Checkable.CheckableItem.Miles(isChecked = false)
        val kilometersOption = SettingItemAction.Checkable.CheckableItem.Kilometers(isChecked = true)
        val settingsItems = listOf(
            SettingItem.DistanceUnits(
                title = 4,
                options = listOf(milesOption, kilometersOption)
            )
        )

        viewModel.state.update { it.copy(settingsData = SettingsData(distanceUnits = MetricsConstants.KILOMETERS_AND_METERS), settingItems = settingsItems) }

        // When
        viewModel.onItemChecked(milesOption)

        // Then
        val actualDistanceUnitSettingItem = (viewModel.state.value.settingItems.first() as SettingItem.DistanceUnits)
        assertTrue(actualDistanceUnitSettingItem.options.first().isChecked)
        assertFalse(actualDistanceUnitSettingItem.options.last().isChecked)
        verify { settingsRepository.saveMetricUnits(MetricsConstants.MILES_AND_FEET) }
    }

    @Test
    fun `When Storage option selected, and the corresponding StorageType is set, then the confirmation should NOT be shown`() {
        listOf(
            SettingItemAction.Checkable.CheckableItem.Phone(),
            SettingItemAction.Checkable.CheckableItem.Card(isAvailable = true),
        ).forEach { option ->
            // Given
            val correspondingStorageType = if (option is SettingItemAction.Checkable.CheckableItem.Phone) {
                StorageType.PHONE
            } else {
                StorageType.SD_CARD
            }
            viewModel.state.value = SettingsViewModel.SettingsState(settingsData = SettingsData(storageType = correspondingStorageType))

            // When
            val showConfirmation = viewModel.shouldShowStorageChangeConfirmation(option)

            // Then
            assertFalse(showConfirmation)
        }
    }

    @Test
    fun `When Storage option selected, and the contrary StorageType is set, then the confirmation SHOULD be shown`() {
        listOf(
            SettingItemAction.Checkable.CheckableItem.Phone(),
            SettingItemAction.Checkable.CheckableItem.Card(isAvailable = true),
        ).forEach { option ->
            // Given
            val contraryStorageType = if (option is SettingItemAction.Checkable.CheckableItem.Phone) {
                StorageType.SD_CARD
            } else {
                StorageType.PHONE
            }
            viewModel.state.value = SettingsViewModel.SettingsState(settingsData = SettingsData(storageType = contraryStorageType))

            // When
            val showConfirmation = viewModel.shouldShowStorageChangeConfirmation(option)

            // Then
            assertTrue(showConfirmation)
        }
    }
}
