package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.services.dto.CategoryDto

data class CreateCategoryRequestBody(
    val category: CategoryDto
)
