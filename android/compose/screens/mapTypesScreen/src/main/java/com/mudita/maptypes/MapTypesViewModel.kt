package com.mudita.maptypes

import androidx.lifecycle.ViewModel
import com.mudita.map.common.enums.MapType
import com.mudita.map.common.model.MapTypeItem
import com.mudita.map.common.model.MapTypesData
import com.mudita.maptypes.repository.MapTypesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MapTypesViewModel @Inject constructor(
    private val mapTypesRepository: MapTypesRepository
) : ViewModel() {
    val state = MutableStateFlow(MapTypesState())

    fun initWithData(mapTypesData: MapTypesData){
        state.update { it.copy(mapTypesData = mapTypesData) }
        getMapTypesItems()
    }

    init {
        getMapTypesItems()
    }
    private fun getMapTypesItems(){
        state.update { it.copy(mapTypeItems = mapTypesRepository.getMapTypesItems()) }
    }

    fun onItemChecked(item: MapTypeItem){
        state.update { it.copy(mapTypesData = MapTypesData(item.mapType)) }
    }

    data class MapTypesState(
        val mapTypeItems : List<MapTypeItem> = listOf(),
        val mapTypesData: MapTypesData = MapTypesData(MapType.DRIVING)
    )
}

