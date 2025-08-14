package com.mudita.download.ui

import androidx.annotation.StringRes
import com.mudita.maps.frontitude.R

enum class ErrorType(@StringRes val titleRes: Int, @StringRes val descriptionRes: Int) {
    Network(
        titleRes = R.string.common_error_dialog_h1_downloadcantstart,
        descriptionRes = R.string.common_error_dialog_body_makesureyouareconnected,
    ),
    WifiNetwork(
        titleRes = R.string.common_error_dialog_h1_downloadcantstart,
        descriptionRes = R.string.common_error_dialog_body_connecttowifi,
    ),
    Memory(
        titleRes = R.string.common_error_dialog_h1_downloadcantstart,
        descriptionRes = R.string.common_error_dialog_body_memoryisfull,
    );
}