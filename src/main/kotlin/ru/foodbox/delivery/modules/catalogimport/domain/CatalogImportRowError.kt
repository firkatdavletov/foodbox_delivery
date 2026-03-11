package ru.foodbox.delivery.modules.catalogimport.domain

data class CatalogImportRowError(
    val rowNumber: Int,
    val rowKey: String?,
    val errorCode: CatalogImportErrorCode,
    val message: String,
)
