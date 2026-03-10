package ru.foodbox.delivery.common.error

data class ApiError(
    val code: String,
    val message: String,
    val traceId: String?,
    val details: Map<String, Any?>? = null
)