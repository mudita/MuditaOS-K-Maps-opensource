package com.mudita.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.kompakt.commonUi.KompaktTypography500
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.colorBlack
import com.mudita.kompakt.commonUi.colorGray_11
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.kompakt.commonUi.components.KompaktRadioButton
import com.mudita.kompakt.commonUi.components.KompaktSwitch
import com.mudita.map.common.model.SettingItem
import com.mudita.map.common.model.SettingItemAction
import com.mudita.map.common.model.SettingsData
import com.mudita.map.common.model.StorageType
import com.mudita.maps.frontitude.R

@Composable
fun SettingItemView(
    settingsData: SettingsData,
    settingItem: SettingItem,
    isLast: Boolean,
    isDownloading: Boolean,
    onItemClicked: (SettingItemAction.Selectable) -> Unit,
    onItemSwitched: (SettingItemAction.Switchable) -> Unit,
    onItemChecked: (SettingItemAction.Checkable.CheckableItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable {
                if (settingItem is SettingItemAction.Selectable) onItemClicked(settingItem)
            }
    ) {
        when (settingItem) {
            is SettingItemAction.Switchable -> Switch(settingItem) {
                onItemSwitched(it)
            }

            is SettingItem.DistanceUnits -> DistanceUnitRadioGroup(settingItem) {
                onItemChecked(it)
            }
            is SettingItem.Storage -> StorageTypeRadioGroup(settingItem, settingsData.storageType, isDownloading) {
                onItemChecked(it)
            }
        }
        if (isLast.not()) DashedHorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
        )
    }
}

@Composable
private fun StorageTypeRadioGroup(
    settingItem: SettingItem,
    currentStorageType: StorageType,
    isDownloading: Boolean,
    onCheck: (SettingItemAction.Checkable.CheckableItem) -> Unit
) {
    if (settingItem !is SettingItemAction.Checkable) return

    val validatedOptions = settingItem.options.map { option ->
        when (option) {
            is SettingItemAction.Checkable.CheckableItem.Phone -> option.isChecked = currentStorageType == StorageType.PHONE
            is SettingItemAction.Checkable.CheckableItem.Card -> option.isChecked = currentStorageType == StorageType.SD_CARD
            else -> Unit
        }
        option
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = settingsItemMinHeight)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 9.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = settingItem.title),
            style = KompaktTypography900.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        RadioGroup(options = validatedOptions, onCheck = onCheck, isOptionEnabled = { it.isAvailable && !isDownloading })

        if (isDownloading) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(id = R.string.common_notification_cantchangestorage),
                style = KompaktTypography500.displaySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DistanceUnitRadioGroup(
    settingItem: SettingItem,
    onCheck: (SettingItemAction.Checkable.CheckableItem) -> Unit
) {
    if (settingItem !is SettingItemAction.Checkable) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = settingsItemMinHeight)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = settingItem.title),
            style = KompaktTypography900.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        RadioGroup(options = settingItem.options, onCheck = onCheck)
    }
}

@Composable
private fun RadioGroup(
    options: List<SettingItemAction.Checkable.CheckableItem>,
    onCheck: (SettingItemAction.Checkable.CheckableItem) -> Unit,
    modifier: Modifier = Modifier,
    isOptionEnabled: (SettingItemAction.Checkable.CheckableItem) -> Boolean = { true },
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val enabled = isOptionEnabled(option)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .widthIn(max = 170.dp)
                    .clickable(enabled = enabled) { onCheck(option) }
                    .padding(top = 8.dp)
            ) {
                KompaktRadioButton(
                    size = 28.dp,
                    isSwitchedOn = option.isChecked,
                    touchAreaPadding = 0.dp,
                    enabled = enabled,
                ) { checked -> if (checked) onCheck(option) }

                Text(
                    text = stringResource(id = option.name),
                    color = if (enabled) colorBlack else colorGray_11,
                    style = KompaktTypography500.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Switch(
    settingItem: SettingItem,
    onSwitch: (SettingItemAction.Switchable) -> Unit
) {
    if (settingItem !is SettingItemAction.Switchable) return
    var isSwitched by remember { mutableStateOf(settingItem.isChecked) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .defaultMinSize(minHeight = settingsItemMinHeight)
            .padding(start = 16.dp, top = 12.dp, bottom = 11.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            Text(
                text = stringResource(id = settingItem.title),
                style = KompaktTypography900.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            settingItem.desc?.let {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = stringResource(id = it),
                    style = KompaktTypography500.labelSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        KompaktSwitch(
            isSwitchedOn = isSwitched,
            height = 30.dp,
            width = 48.dp,
            horizontalTouchAreaPadding = 16.dp,
            verticalTouchAreaPadding = 8.dp
        ) {
            onSwitch(
                when (settingItem) {
                    is SettingItem.Sound -> settingItem.copy(isChecked = isSwitched.not())
                    is SettingItem.ScreenAlwaysOn -> settingItem.copy(isChecked = isSwitched.not())
                    is SettingItem.WifiOnly -> settingItem.copy(isChecked = isSwitched.not())
                    else -> settingItem
                }
            )
            isSwitched = !isSwitched
        }
    }
}

@Composable
@Preview(backgroundColor = 0xffffffff, showBackground = true)
fun SettingItemViewPreview() {
    Column {
        SettingItemView(
            settingsData = SettingsData(
                soundEnabled = true,
                storageType = StorageType.SD_CARD
            ),
            settingItem = SettingItem.ScreenAlwaysOn(
                R.string.common_toggle_button_h1_screenalwayson,
                R.string.maps_menu_toggle_button_body_stopsthescreenlocking,
            ),
            isLast = false,
            isDownloading = false,
            onItemClicked = {},
            onItemSwitched = {},
            onItemChecked = {}
        )
        SettingItemView(
            settingsData = SettingsData(
                soundEnabled = true,
                storageType = StorageType.SD_CARD
            ),
            settingItem = SettingItem.ScreenAlwaysOn(
                R.string.common_toggle_button_h1_wifionly,
                R.string.maps_menu_toggle_button_body_offlinemapswill,
            ),
            isLast = false,
            isDownloading = false,
            onItemClicked = {},
            onItemSwitched = {},
            onItemChecked = {}
        )
        SettingItemView(
            settingsData = SettingsData(
                soundEnabled = true,
                storageType = StorageType.SD_CARD
            ),
            settingItem = SettingItem.Storage(
                title = R.string.common_menuitem_h1_storage,
                desc = null,
                options = listOf(
                    SettingItemAction.Checkable.CheckableItem.Card(isAvailable = false),
                    SettingItemAction.Checkable.CheckableItem.Phone(),
                )
            ),
            isLast = false,
            isDownloading = true,
            onItemClicked = {},
            onItemSwitched = {},
            onItemChecked = {}
        )
        SettingItemView(
            settingsData = SettingsData(
                soundEnabled = true,
                storageType = StorageType.PHONE
            ),
            settingItem = SettingItem.DistanceUnits(
                title = R.string.common_label_distanceunits,
                desc = null,
                options = listOf(
                    SettingItemAction.Checkable.CheckableItem.Miles(isChecked = false),
                    SettingItemAction.Checkable.CheckableItem.Kilometers(isChecked = true),
                )
            ),
            isLast = false,
            isDownloading = false,
            onItemClicked = {},
            onItemSwitched = {},
            onItemChecked = {}
        )
    }
}