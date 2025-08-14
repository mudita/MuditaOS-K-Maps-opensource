package com.mudita.maps.data.api

sealed class Resource<out T>{
    data class Success<T>(val model: T): Resource<T>()
    data class Error<T>(val throwable: Throwable, var model : T? = null): Resource<T>()
    data class Loading(val progressFraction: Double = 0.0, val chunkFinished: Boolean = false): Resource<Nothing>()
}

fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> =
    when (this) {
        is Resource.Success -> {
            try {
                Resource.Success(transform(model))
            } catch (e: Exception) {
                Resource.Error(e)
            }
        }
        is Resource.Error -> Resource.Error(throwable, model?.let(transform))
        is Resource.Loading -> this
    }

fun <T, R> Resource<T>.mapCatching(transform: (T) -> R): Resource<R> =
    try {
        map(transform)
    } catch (e: Exception) {
        Resource.Error(e)
    }

val <T> Resource<T>.modelOrNull: T?
    get() = when(this) {
        is Resource.Error -> model
        is Resource.Loading -> null
        is Resource.Success -> model
    }
