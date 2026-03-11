package ru.foodbox.delivery.modules.catalogimport.domain.model

data class CategoryImportRow(
    val rowNumber: Int,
    val externalId: String,
    val name: String,
    val slug: String,
    val parentExternalId: String?,
    val description: String?,
    val isActive: Boolean,
    val sortOrder: Int,
)
