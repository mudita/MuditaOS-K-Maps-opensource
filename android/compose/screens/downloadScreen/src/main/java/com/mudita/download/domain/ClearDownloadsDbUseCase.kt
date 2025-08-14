package com.mudita.download.domain

interface ClearDownloadsDbUseCase {

    suspend operator fun invoke()

}