package com.mudita.download.repository.models

sealed interface MapFile {
    fun getBasename(): String
}