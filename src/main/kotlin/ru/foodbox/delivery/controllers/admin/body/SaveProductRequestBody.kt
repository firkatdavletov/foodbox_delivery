package ru.foodbox.delivery.controllers.admin.body

import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto

class SaveProductRequestBody(
    val product: ProductDto
)