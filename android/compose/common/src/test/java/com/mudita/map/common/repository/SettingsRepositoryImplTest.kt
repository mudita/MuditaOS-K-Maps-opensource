package com.mudita.map.common.repository

import com.mudita.map.common.enums.MetricsConstants
import com.mudita.map.common.model.SettingItem
import com.mudita.map.common.model.SettingItemAction
import com.mudita.map.common.sharedPrefs.MetricUnitPreference
import com.mudita.map.common.sharedPrefs.SettingsPreference
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SettingsRepositoryImplTest {

    @RelaxedMockK private lateinit var metricUnitPreference: MetricUnitPreference
    @RelaxedMockK private lateinit var settingsPreference: SettingsPreference

    private lateinit var settingsRepository: SettingsRepository

    @BeforeEach
    fun setup() {
        settingsRepository = SettingsRepositoryImpl(
            metricUnitPreference,
            settingsPreference,
        )
    }

    @Test
    fun `Given wifi only option is set to true, when getSettingsItems called, should return this option as checked`() {
        // Given
        every { settingsPreference.getWifiOnlyEnabled() } returns true

        // When
        val result = settingsRepository.getSettingsItems(hasSDCard = false)

        // Then
        val screenAlwaysOnOption = result.find { it is SettingItem.WifiOnly } as SettingItem.WifiOnly
        assertTrue(screenAlwaysOnOption.isChecked)
    }


    @Test
    fun `Given distance unit is set to kilometers, when getSettingsItems called, should return kilometers option as selected`() {
        // Given
        every { metricUnitPreference.getMetricUnit() } returns "km-m"

        // When
        val result = settingsRepository.getSettingsItems(hasSDCard = false)

        // Then
        val distanceUnitsOptions = (result.find { it is SettingItem.DistanceUnits } as SettingItem.DistanceUnits).options
        assertInstanceOf(SettingItemAction.Checkable.CheckableItem.Miles::class.java, distanceUnitsOptions.first())
        assertFalse(distanceUnitsOptions.first().isChecked)
        assertInstanceOf(SettingItemAction.Checkable.CheckableItem.Kilometers::class.java, distanceUnitsOptions.last())
        assertTrue(distanceUnitsOptions.last().isChecked)
    }

    @Test
    fun `Given distance unit is set to miles, when getSettingsItems called, should return miles option as selected`() {
        // Given
        every { metricUnitPreference.getMetricUnit() } returns "mi-f"

        // When
        val result = settingsRepository.getSettingsItems(hasSDCard = false)

        // Then
        val distanceUnitsOptions = (result.find { it is SettingItem.DistanceUnits } as SettingItem.DistanceUnits).options
        assertInstanceOf(SettingItemAction.Checkable.CheckableItem.Miles::class.java, distanceUnitsOptions.first())
        assertTrue(distanceUnitsOptions.first().isChecked)
        assertInstanceOf(SettingItemAction.Checkable.CheckableItem.Kilometers::class.java, distanceUnitsOptions.last())
        assertFalse(distanceUnitsOptions.last().isChecked)
    }

    @Test
    fun `Given sd card is available, when getSettingsItems called, should return sd card option as available`() {
        // Given
        val hasSDCard = true

        // When
        val result = settingsRepository.getSettingsItems(hasSDCard = hasSDCard)

        // Then
        val storageOptions = (result.find { it is SettingItem.Storage } as SettingItem.Storage).options
        assertInstanceOf(SettingItemAction.Checkable.CheckableItem.Card::class.java, storageOptions.first())
        assertTrue(storageOptions.first().isAvailable)
        assertInstanceOf(SettingItemAction.Checkable.CheckableItem.Phone::class.java, storageOptions.last())
    }

    @Test
    fun `Given sd card is not available, when getSettingsItems called, should return sd card option as unavailable`() {
        // Given
        val hasSDCard = false

        // When
        val result = settingsRepository.getSettingsItems(hasSDCard = hasSDCard)

        // Then
        val storageOptions = (result.find { it is SettingItem.Storage } as SettingItem.Storage).options
        assertInstanceOf(SettingItemAction.Checkable.CheckableItem.Card::class.java, storageOptions.first())
        assertFalse(storageOptions.first().isAvailable)
        assertInstanceOf(SettingItemAction.Checkable.CheckableItem.Phone::class.java, storageOptions.last())
    }

    @Test
    fun `When getSoundEnabled called, should return saved value for this option`() {
        // Given
        every { settingsPreference.getSoundEnabled() } returns true

        // When
        val result = settingsRepository.getSoundEnabled()

        // Then
        assertTrue(result)
        verify { settingsPreference.getSoundEnabled() }
    }

    @Test
    fun `When getScreenAlwaysOn called, should return saved value for this option`() {
        // Given
        every { settingsPreference.getScreenAlwaysOnEnabled() } returns true

        // When
        val result = settingsRepository.getScreenAlwaysOn()

        // Then
        assertTrue(result)
        verify { settingsPreference.getScreenAlwaysOnEnabled() }
    }

    @Test
    fun `When saveSoundEnabled called, should save value for this option`() {
        // When
        settingsRepository.saveSoundEnabled(true)

        // Then
        verify { settingsPreference.setSoundEnabled(true) }
    }

    @Test
    fun `When saveScreenAlwaysOnEnabled called, should save value for this option`() {
        // When
        settingsRepository.saveScreenAlwaysOnEnabled(true)

        // Then
        verify { settingsPreference.setScreenAlwaysOnEnabled(true) }
    }

    @Test
    fun `When saveWifiOnlyEnabled called, should save value for this option`() {
        // When
        settingsRepository.saveWifiOnlyEnabled(true)

        // Then
        verify { settingsPreference.setWifiOnlyEnabled(true) }
    }

    @Test
    fun `Given miles as distance unit, when saveMetricUnits called, should save value for this option`() {
        // Given
        val units = MetricsConstants.MILES_AND_FEET

        // When
        settingsRepository.saveMetricUnits(units)

        // Then
        verify { metricUnitPreference.setMetricUnit("mi-f") }
    }

    @Test
    fun `Given kilometers as distance unit, when saveMetricUnits called, should save value for this option`() {
        // Given
        val units = MetricsConstants.KILOMETERS_AND_METERS

        // When
        settingsRepository.saveMetricUnits(units)

        // Then
        verify { metricUnitPreference.setMetricUnit("km-m") }
    }

    @Test
    fun `When registerOnSoundChangedListener called, should register sound change listener`() {
        // Given
        val listener: (Boolean) -> Unit = {}

        // When
        settingsRepository.registerOnSoundChangedListener(listener)

        // Then
        verify { settingsPreference.registerOnSoundChangedListener(listener) }
    }

    @Test
    fun `When unregisterOnSoundChangedListener called, should unregister sound change listener`() {
        // Given
        val listener: (Boolean) -> Unit = {}

        // When
        settingsRepository.unregisterOnSoundChangedListener(listener)

        // Then
        verify { settingsPreference.unregisterOnSoundChangedListener(listener) }
    }
}