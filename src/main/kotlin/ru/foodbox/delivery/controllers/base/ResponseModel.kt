package ru.foodbox.delivery.controllers.base

interface ResponseModel<T> {
    val success: Boolean
    val error: String?
    val code: Int?
}

open class BaseResponseModel<T>(
    open val data: T?,
    open val success: Boolean,
    open val error: String?,
    open val code: Int?,
) {
    constructor(data: T) : this(data, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}