package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.services.dto.ProductDto

data class CreateProductResponseBody(
    val product: ProductDto
)
