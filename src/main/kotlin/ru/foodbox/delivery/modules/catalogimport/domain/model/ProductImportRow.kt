package ru.foodbox.delivery.modules.catalogimport.domain.model

data class ProductImportRow(
    val rowNumber: Int,
    val externalId: String,
    val sku: String,
    val name: String,
    val slug: String?,
    val description: String?,
    val categoryExternalId: String,
    val priceMinor: Long,
    val oldPriceMinor: Long?,
    val brand: String?,
    val isActive: Boolean,
    val imageUrl: String?,
    val sortOrder: Int,
)
