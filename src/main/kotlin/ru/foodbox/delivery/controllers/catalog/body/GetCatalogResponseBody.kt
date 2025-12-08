package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto

data class GetCatalogResponseBody(
    val catalog: List<CategoryDto>,
)