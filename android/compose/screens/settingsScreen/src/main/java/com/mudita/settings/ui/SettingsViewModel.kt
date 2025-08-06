package com.mudita.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.map.common.enums.MetricsConstants
import com.mudita.map.common.model.SettingItem
import com.mudita.map.common.model.SettingItemAction
import com.mudita.map.common.model.SettingItemAction.Checkable.CheckableItem
import com.mudita.map.common.model.SettingsData
import com.mudita.map.common.model.StorageType
import com.mudita.map.common.repository.SettingsRepository
import com.mudita.map.common.sharedPrefs.SDCardPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sdCardPreferencesManager: SDCardPreferencesManager
) : ViewModel() {
    val state = MutableStateFlow(SettingsState())

    init {
        sdCardPreferencesManager.isSDCardEnabled
            .flowOn(Dispatchers.IO)
            .onEach {
                getSettingsItems(it)
            }
            .launchIn(viewModelScope)
    }

    fun initSettings(settingsData: SettingsData) {
        state.update { it.copy(settingsData = settingsData) }
        updateSDCardEnabled(settingsData.sdCardExists)
    }

    fun refreshState() {
        getSettingsItems(state.value.settingsData.sdCardExists)
    }

    private fun updateSDCardEnabled(value: Boolean) {
        viewModelScope.launch {
            sdCardPreferencesManager.updateSDCardEnabled(value)
        }
    }

    private fun getSettingsItems(hasSDCard: Boolean) {
        val settingsData = state.value.settingsData.copy(
            sdCardExists = hasSDCard,
            storageType = if (hasSDCard) state.value.settingsData.storageType else StorageType.PHONE,
        )
        state.update { it.copy(settingItems = settingsRepository.getSettingsItems(hasSDCard), settingsData = settingsData) }
    }

    fun onItemSwitched(settingItem: SettingItemAction.Switchable) {
        when (settingItem) {
            is SettingItem.ScreenAlwaysOn -> settingsRepository.saveScreenAlwaysOnEnabled(settingItem.isChecked)
            is SettingItem.Sound -> settingsRepository.saveSoundEnabled(settingItem.isChecked.not()) // isSoundEnabled = isMuted.not() = isChecked.not()
            is SettingItem.WifiOnly -> settingsRepository.saveWifiOnlyEnabled(settingItem.isChecked)
        }
    }

    fun shouldShowStorageChangeConfirmation(settingItem: CheckableItem): Boolean =
        when (settingItem) {
            is CheckableItem.Phone -> state.value.settingsData.storageType != StorageType.PHONE
            is CheckableItem.Card -> state.value.settingsData.storageType != StorageType.SD_CARD
            else -> false
        }

    fun onItemChecked(settingItem: CheckableItem) {
        when(settingItem) {
            is CheckableItem.Card -> {
                state.update {
                    it.copy(settingsData = state.value.settingsData.copy(storageType = StorageType.SD_CARD))
                }
            }
            is CheckableItem.Phone -> {
                state.update {
                    it.copy(settingsData = state.value.settingsData.copy(storageType = StorageType.PHONE))
                }
            }
            is CheckableItem.Kilometers -> {
                updateDistanceUnitsSettings(true)
            }
            is CheckableItem.Miles -> {
                updateDistanceUnitsSettings(false)
            }
            else -> Unit
        }
    }

    private fun updateDistanceUnitsSettings(kilometers: Boolean) {
        val updatedSettingsItems = state.value.settingItems.map { item ->
            when(item) {
                is SettingItem.DistanceUnits -> item.copy(options = item.options.map { option ->
                    when (option) {
                        is CheckableItem.Kilometers -> CheckableItem.Kilometers(isChecked = kilometers)
                        is CheckableItem.Miles -> CheckableItem.Miles(isChecked = !kilometers)
                        else -> option
                    }
                })
                else -> item
            }
        }
        state.update {
            it.copy(settingItems = updatedSettingsItems)
        }
        val constant = if (kilometers) MetricsConstants.KILOMETERS_AND_METERS else MetricsConstants.MILES_AND_FEET
        settingsRepository.saveMetricUnits(constant)
    }

    data class SettingsState(
        val settingItems: List<SettingItem> = emptyList(),
        val settingsData: SettingsData = SettingsData()
    )
}