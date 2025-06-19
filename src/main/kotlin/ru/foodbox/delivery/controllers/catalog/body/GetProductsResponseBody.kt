package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto

data class GetProductsResponseBody(
    val category: CategoryDto,
    val products: List<ProductDto>
)