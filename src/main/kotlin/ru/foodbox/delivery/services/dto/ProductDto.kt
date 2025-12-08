package ru.foodbox.delivery.services.dto

data class ProductDto(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val description: String?,
    val price: Double,
    val imageUrl: String?
)
