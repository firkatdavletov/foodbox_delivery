package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.services.dto.CategoryDto

data class CreateCategoryResponseBody(
    val category: CategoryDto,
)
