package ru.foodbox.delivery.services.dto

data class CategoryDto(
    val id: Long,
    val parentCategory: Long?,
    val title: String,
    val imageUrl: String?,
    val products: List<ProductDto>,
    val span: Int,
)
