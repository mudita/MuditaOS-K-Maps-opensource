package net.osmand.plus.activities.viewmodels

import androidx.lifecycle.ViewModel
import com.mudita.download.domain.GetDownloadsQueueUseCase
import com.mudita.download.repository.mappers.toDownloadItems
import com.mudita.download.ui.Action
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform

@HiltViewModel
class MapActivityViewModel @Inject constructor(
    private val getDownloadsQueueUseCase: GetDownloadsQueueUseCase,
) : ViewModel() {

    fun downloadAction(): Flow<Action> =
        flow { emit(getDownloadsQueueUseCase()) }
            .transform { result ->
                val queueModel = result.getOrNull()
                if (queueModel.isNullOrEmpty()) return@transform
                emit(Action.Download(queueModel.toDownloadItems()))
            }
}