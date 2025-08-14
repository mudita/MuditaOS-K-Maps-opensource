package com.mudita.map.common.model.routing

data class RoadDetails(
    val roadNumber: String? = null,
    val name: String? = null,
) {
    fun areDetailsComplete(): Boolean = roadNumber != null && name != null
    fun areAnyDetailsAvailable(): Boolean = !roadNumber.isNullOrBlank() || !name.isNullOrBlank()
}
