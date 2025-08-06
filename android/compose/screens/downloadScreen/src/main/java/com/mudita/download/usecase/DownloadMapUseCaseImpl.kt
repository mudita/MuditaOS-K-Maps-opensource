package com.mudita.download.usecase

import com.mudita.download.domain.DownloadMapUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.Downloadable
import com.mudita.maps.data.api.Resource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DownloadMapUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : DownloadMapUseCase {

    override fun invoke(map: Downloadable): Flow<Resource<Unit>> {
        return downloadRepository.downloadMap(map)
    }
}