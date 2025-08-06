package com.mudita.map.common.utils

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber

class VibrationFeedbackManagerImpl(
    @ApplicationContext private val context: Context,
) : VibrationFeedbackManager {

    private val vibrator: Vibrator = (context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator

    override fun vibrate() {
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager?
        if (audioManager == null || audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        try {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (exc: Exception) {
            Timber.e(exc)
        }
    }

    companion object {
        private const val VIBRATION_DURATION = 300L
    }
}
