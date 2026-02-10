package ru.foodbox.delivery.services.dto

data class CategoryDto(
    val id: Long,
    val parentCategory: Long?,
    var title: String,
    var imageUrl: String?,
    val products: List<ProductDto> = emptyList(),
    val children: List<CategoryDto> = emptyList(),
)
