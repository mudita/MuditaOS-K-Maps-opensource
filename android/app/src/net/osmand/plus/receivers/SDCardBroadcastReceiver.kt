package net.osmand.plus.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mudita.map.common.sharedPrefs.SDCardPreferencesManager
import com.mudita.map.common.sharedPrefs.SDCardPreferencesManagerImpl
import java.io.File
import kotlinx.coroutines.runBlocking
import net.osmand.IProgress
import net.osmand.plus.OsmandApplication
import net.osmand.plus.resources.ResourceManager
import net.osmand.plus.settings.datastorage.DataStorageHelper
import net.osmand.plus.utils.FileUtils

class SDCardBroadcastReceiver(private val storageHelper: DataStorageHelper) : BroadcastReceiver() {

    private lateinit var sdCardPreferencesManager: SDCardPreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        sdCardPreferencesManager = SDCardPreferencesManagerImpl(context)
        val app: OsmandApplication = context.applicationContext as OsmandApplication
        val resourceManager = checkNotNull(app.resourceManager)
        if (intent.action != null && intent.action == UNMOUNTED || intent.action == EJECT || intent.action == BAD_REMOVAL || intent.action == REMOVE) {
            setPhoneStorageAsDefault(storageHelper, app)
            updateSDCardEnabled(false, resourceManager)
        } else if (intent.action != null && intent.action == MOUNTED) {
            updateSDCardEnabled(true, resourceManager)
        }
    }

    private fun updateSDCardEnabled(value: Boolean, resourceManager: ResourceManager) = runBlocking {
        storageHelper.updateStorageItems()
        sdCardPreferencesManager.updateSDCardEnabled(value)
        resourceManager.reloadIndexesAsync(IProgress.EMPTY_PROGRESS, null)
    }

    private fun setPhoneStorageAsDefault(storageHelper: DataStorageHelper, app: OsmandApplication){
        val externalStorageItems = storageHelper.storageItems.filter { it.type == 1 }
        val phoneStorageItem = externalStorageItems.firstOrNull()
        phoneStorageItem?.let {
            val newDirectory = it.directory
            val type = it.type
            val newDirectoryFile = File(newDirectory)
            val wr = FileUtils.isWritable(newDirectoryFile)
            if (wr) {
                app.setExternalStorageDirectory(type, newDirectory)
                app.resourceManager?.reloadIndexesAsync(IProgress.EMPTY_PROGRESS, null)
            }
        }
    }

    companion object {
        private const val MOUNTED = "android.intent.action.MEDIA_MOUNTED"
        private const val UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED"
        private const val EJECT = "android.intent.action.MEDIA_EJECT"
        private const val BAD_REMOVAL = "android.intent.action.MEDIA_BAD_REMOVAL"
        private const val REMOVE = "android.intent.action.MEDIA_REMOVED"

        fun getIntentFilter(): IntentFilter {
            val filter = IntentFilter()
            filter.addDataScheme("file")
            filter.addAction(MOUNTED)
            filter.addAction(UNMOUNTED)
            filter.addAction(EJECT)
            filter.addAction(BAD_REMOVAL)
            filter.addAction(REMOVE)
            return filter
        }
    }
}