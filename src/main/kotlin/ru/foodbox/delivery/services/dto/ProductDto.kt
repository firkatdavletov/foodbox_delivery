package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.services.model.UnitOfMeasure

data class ProductDto(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val description: String?,
    val price: Long,
    val oldPrice: Long? = null,
    val discountLabel: String? = null,
    val imageUrl: String?,
    val unit: UnitOfMeasure,
    val displayWeight: String?,
    val countStep: Int,
    val sku: String? = null,
    val brandName: String? = null,
)
