package net.osmand.plus.download

import com.mudita.download.domain.DeleteMapUseCase
import com.mudita.download.repository.DownloadRepository
import com.mudita.download.repository.models.LocalIndex
import com.mudita.download.repository.models.MapFile
import javax.inject.Inject
import net.osmand.plus.OsmandApplication

class OsmAndDeleteMapUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val application: OsmandApplication,
) : DeleteMapUseCase {

    override suspend fun invoke(map: MapFile) {
        (map as? LocalIndex)?.let {
            application.resourceManager?.closeFile(it.file.name)
        }
        downloadRepository.deleteMap(map)
    }
}