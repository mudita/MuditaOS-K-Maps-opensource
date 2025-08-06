package com.mudita.searchhistory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.map.common.model.SearchItemData
import com.mudita.map.common.utils.OsmAndFormatter
import com.mudita.searchhistory.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.osmand.data.LatLon

@HiltViewModel
class SearchHistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val osmAndFormatter: OsmAndFormatter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchHistoryState())
    val uiState: StateFlow<SearchHistoryState> = _uiState.asStateFlow()

    fun getHistory() {
        viewModelScope.launch {
            // TODO pass a proper location when this screen is used again in the app.
            historyRepository.getHistory(LatLon.zero).onSuccess { historyFlow ->
                historyFlow
                    .onEach { historyItems ->
                        _uiState.tryEmit(_uiState.value.copy(items = historyItems))
                    }
                    .flowOn(Dispatchers.Main)
                    .launchIn(this)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun formatDistance(searchItemData: SearchItemData): String = searchItemData.formattedDistance(osmAndFormatter)

    data class SearchHistoryState(
        val items: List<SearchItemData> = emptyList(),
    )

}