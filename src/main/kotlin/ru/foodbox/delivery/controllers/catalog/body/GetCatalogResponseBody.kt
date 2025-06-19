package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto

data class GetCatalogResponseBody(
    val categories: List<CategoryDto>,
    val products: List<ProductDto>
)