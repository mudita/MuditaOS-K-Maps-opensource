package net.osmand.plus.settings.datastorage.item

import com.mudita.map.common.model.StorageType

fun StorageItem.getStorageType(): StorageType = if (key.contains("1")) StorageType.PHONE else StorageType.SD_CARD
