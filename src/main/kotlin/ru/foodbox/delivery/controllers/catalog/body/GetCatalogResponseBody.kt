package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.services.dto.CategoryDto

data class GetCatalogResponseBody(
    val catalog: List<CategoryDto>,
)