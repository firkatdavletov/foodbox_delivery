package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.services.dto.ProductDto
import java.math.BigDecimal

@Component
class ProductMapper {
    fun toDto(entity: ProductEntity) = ProductDto(
        id = entity.id!!,
        categoryId = entity.category.id!!,
        price = entity.price
            .multiply(BigDecimal(100))
            .longValueExact(),
        title = entity.title,
        description = entity.description,
        imageUrl = entity.imageUrl,
        unit = entity.unit,
        countStep = entity.countStep,
        displayWeight = entity.displayWeight
    )

    fun toDto(entities: List<ProductEntity>) = entities.map {
        toDto(it)
    }
}