package com.mudita.map.common.di

import android.content.Context
import com.mudita.map.common.utils.VibrationFeedbackManager
import com.mudita.map.common.utils.VibrationFeedbackManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeedbackManagersModule {

    @Provides
    @Singleton
    fun provideVibrationFeedbackManager(
        @ApplicationContext application: Context,
    ): VibrationFeedbackManager {
        return VibrationFeedbackManagerImpl(application)
    }
}
