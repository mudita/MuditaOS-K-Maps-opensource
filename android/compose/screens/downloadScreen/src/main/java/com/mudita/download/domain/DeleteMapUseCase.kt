package com.mudita.download.domain

import com.mudita.download.repository.models.MapFile

interface DeleteMapUseCase {

    suspend operator fun invoke(map: MapFile)

}