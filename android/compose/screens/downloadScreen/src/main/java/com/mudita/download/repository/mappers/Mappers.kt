package com.mudita.download.repository.mappers

import com.mudita.download.repository.models.DownloadItem
import com.mudita.download.repository.models.DownloadQueueModel
import com.mudita.maps.data.db.entity.DownloadEntity

fun DownloadQueueModel.toEntity() = DownloadEntity(
    filename = filename,
    name = name,
    description = description,
    size = size,
    targetSize = targetSize,
    parentName = parentName,
)

fun DownloadEntity.toQueueModel() = DownloadQueueModel(
    filename = filename,
    name = name,
    description = description,
    size = size,
    targetSize = targetSize,
    parentName = parentName,
)

fun DownloadItem.toQueueModel() = DownloadQueueModel(
    name = name,
    description = description,
    filename = fileName,
    size = size,
    targetSize = targetSize,
    parentName = parentName,
)

fun List<DownloadEntity>.toQueueModels() = map { it.toQueueModel() }

fun List<DownloadQueueModel>.toDownloadItems() = map {
    DownloadItem(
        name = it.name,
        description = it.description,
        fileName = it.filename,
        size = it.size,
        targetSize = it.targetSize,
        parentName = it.parentName,
    )
}