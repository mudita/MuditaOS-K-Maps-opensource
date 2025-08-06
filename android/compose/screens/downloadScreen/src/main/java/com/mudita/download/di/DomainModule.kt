package com.mudita.download.di

import com.mudita.download.domain.AddDownloadToDbUseCase
import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.domain.ClearDownloadsDbUseCase
import com.mudita.download.domain.DeleteDownloadFromDbUseCase
import com.mudita.download.domain.DownloadMapUseCase
import com.mudita.download.domain.DownloadingStatePublisher
import com.mudita.download.domain.GetDownloadsQueueUseCase
import com.mudita.download.domain.GetIndexesUseCase
import com.mudita.download.domain.RemoveDownloadEntryUseCase
import com.mudita.download.domain.SkipDownloadUseCase
import com.mudita.download.usecase.AddDownloadToDbUseCaseImpl
import com.mudita.download.usecase.CancelDownloadUseCaseImpl
import com.mudita.download.usecase.ClearDownloadsDbUseCaseImpl
import com.mudita.download.usecase.DeleteDownloadFromDbUseCaseImpl
import com.mudita.download.usecase.DownloadMapUseCaseImpl
import com.mudita.download.usecase.GetDownloadsQueueUseCaseImpl
import com.mudita.download.usecase.GetIndexesUseCaseImpl
import com.mudita.download.usecase.RemoveDownloadEntryUseCaseImpl
import com.mudita.download.usecase.SkipDownloadUseCaseImpl
import com.mudita.map.common.download.GetDownloadErrorNotificationUseCase
import com.mudita.map.common.download.GetDownloadProgressUseCase
import com.mudita.map.common.download.GetDownloadingStateUseCase
import com.mudita.map.common.download.OnDownloadFinishUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    abstract fun bindGetIndexesUseCase(impl: GetIndexesUseCaseImpl): GetIndexesUseCase

    @Binds
    abstract fun bindDownloadMapUseCase(impl: DownloadMapUseCaseImpl): DownloadMapUseCase

    @Binds
    abstract fun bindCancelDownloadUseCase(impl: CancelDownloadUseCaseImpl): CancelDownloadUseCase

    @Binds
    abstract fun bindAddDownloadToDbUseCase(impl: AddDownloadToDbUseCaseImpl): AddDownloadToDbUseCase

    @Binds
    abstract fun bindDeleteDownloadFromDbUseCase(impl: DeleteDownloadFromDbUseCaseImpl): DeleteDownloadFromDbUseCase

    @Binds
    abstract fun bindClearDownloadsDbUseCase(impl: ClearDownloadsDbUseCaseImpl): ClearDownloadsDbUseCase

    @Binds
    abstract fun bindGetDownloadsQueueUseCase(impl: GetDownloadsQueueUseCaseImpl): GetDownloadsQueueUseCase

    @Binds
    abstract fun bindSkipDownloadUseCase(impl: SkipDownloadUseCaseImpl): SkipDownloadUseCase

    @Binds
    abstract fun bindRemoveDownloadEntryUseCase(impl: RemoveDownloadEntryUseCaseImpl): RemoveDownloadEntryUseCase

    companion object {
        @Provides
        fun provideGetDownloadingStateUseCase(publisher: DownloadingStatePublisher): GetDownloadingStateUseCase =
            publisher.getDownloadingStateUseCase

        @Provides
        fun provideGetDownloadProgressUseCase(publisher: DownloadingStatePublisher): GetDownloadProgressUseCase =
            publisher.getDownloadProgressUseCase

        @Provides
        fun provideOnDownloadFinishUseCase(publisher: DownloadingStatePublisher): OnDownloadFinishUseCase =
            publisher.onDownloadFinishUseCase

        @Provides
        fun provideErrorNotificationUseCase(publisher: DownloadingStatePublisher): GetDownloadErrorNotificationUseCase =
            publisher.errorNotificationUseCase
    }
}