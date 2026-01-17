package ru.foodbox.delivery.utils

sealed class ResultModel<out T> {
    data class Success<out T>(val data: T) : ResultModel<T>()
    data class Error(val message: String?, val errorCode: Int? = null) : ResultModel<Nothing>()
}