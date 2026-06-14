package com.koke1024.craftdice.core

sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>

    data class Success<T>(
        val data: T,
    ) : Resource<T>

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : Resource<Nothing>

    fun isSuccess(): Boolean = this is Success

    fun isError(): Boolean = this is Error

    fun isLoading(): Boolean = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}
