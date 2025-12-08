package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.services.dto.ProductDto

data class CreateProductRequestBody(
    val product: ProductDto
)