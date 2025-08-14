package com.mudita.map.window

import android.app.Activity
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun HideStatusBarEffect(enabled: Boolean = true, restoreOnDispose: Boolean = true) {
    val insetsController = (LocalContext.current as? Activity)?.window?.insetsController ?: return
    DisposableEffect(enabled) {
        if (enabled) {
            insetsController.hideStatusBar()
        } else {
            insetsController.showStatusBar()
        }
        onDispose { if (restoreOnDispose) insetsController.showStatusBar() }
    }
}

private fun WindowInsetsController.showStatusBar() {
    show(WindowInsets.Type.statusBars())
}

private fun WindowInsetsController.hideStatusBar() {
    hide(WindowInsets.Type.statusBars())
}
