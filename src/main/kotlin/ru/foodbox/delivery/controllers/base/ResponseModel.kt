package ru.foodbox.delivery.controllers.base

interface ResponseModel {
    val success: Boolean
    val error: String?
    val code: Int?
}

class BaseResponseModel<T>(
    val data: T?,
    val success: Boolean,
    val error: String?,
    val code: Int?,
) {
    constructor(data: T) : this(data, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}