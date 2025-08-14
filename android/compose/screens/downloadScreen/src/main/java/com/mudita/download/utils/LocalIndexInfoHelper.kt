package com.mudita.download.utils

import com.mudita.download.ui.DownloadViewModel
import net.osmand.IndexConstants
import com.mudita.download.repository.models.LocalIndex
import net.osmand.map.OsmandRegions
import java.io.File
import java.util.*

private fun listFilesSorted(dir: File): Array<File?> {
    val listFiles = dir.listFiles() ?: return arrayOfNulls(0)
    Arrays.sort(listFiles)
    return listFiles
}

private fun loadObfDataImpl(
    dataFile: File,
    result: MutableList<LocalIndex>,
    indexFileNames: Map<String, String>,
    parentName: String,
) {
    val fileName = dataFile.name
    val lt = DownloadViewModel.LocalIndexType.MAP_DATA
    val info = LocalIndex(lt, dataFile, parentName)
    if (indexFileNames.containsKey(fileName)) {
        info.isLoaded = true
    }
    result.add(info)
}

fun loadObfData(
    dataPath: File,
    result: MutableList<LocalIndex>,
    readFiles: Boolean,
    indexFileNames: Map<String, String>,
    indexFiles: Map<String, File>,
    osmandRegions: OsmandRegions,
) {
    if ((readFiles) && dataPath.canRead()) {
        for (mapFile in listFilesSorted(dataPath)) {
            if (mapFile?.isFile == true && mapFile.name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
                loadObfDataImpl(
                    dataFile = mapFile,
                    result = result,
                    indexFileNames = indexFileNames,
                    parentName = osmandRegions.getRegionDataByDownloadName(mapFile.name.substringBeforeLast("."))?.superregion?.localeName ?: ""
                )
            }
        }
    } else {
        for (file in indexFiles.values) {
            if (file.isFile && dataPath.path == file.parent && file.name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
                loadObfDataImpl(
                    dataFile = file,
                    result = result,
                    indexFileNames = indexFileNames,
                    parentName = osmandRegions.getRegionDataByDownloadName(file.name.substringBeforeLast("."))?.superregion?.localeName ?: ""
                )
            }
        }
    }
}