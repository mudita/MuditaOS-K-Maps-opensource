package com.mudita.download.usecase

import com.mudita.download.domain.CancelDownloadUseCase
import com.mudita.download.repository.DownloadRepository
import javax.inject.Inject

class CancelDownloadUseCaseImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : CancelDownloadUseCase {

    override fun invoke() {
        downloadRepository.cancelDownload()
    }

}