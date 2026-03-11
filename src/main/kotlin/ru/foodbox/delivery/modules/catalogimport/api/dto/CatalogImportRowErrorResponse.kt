package ru.foodbox.delivery.modules.catalogimport.api.dto

data class CatalogImportRowErrorResponse(
    val rowNumber: Int,
    val rowKey: String?,
    val errorCode: String,
    val message: String,
)
