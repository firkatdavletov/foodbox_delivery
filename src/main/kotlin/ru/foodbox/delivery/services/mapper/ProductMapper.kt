package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.services.dto.ProductDto

@Component
class ProductMapper {
    fun toDto(entity: ProductEntity) = ProductDto(
        id = entity.id,
        categoryId = entity.categoryId,
        price = entity.price.toFloat(),
        title = entity.title,
        description = entity.description,
        imageUrl = entity.imageUrl,
    )

    fun toDto(entities: List<ProductEntity>) = entities.map {
        toDto(it)
    }
}