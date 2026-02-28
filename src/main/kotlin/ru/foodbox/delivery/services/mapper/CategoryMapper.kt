package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.services.dto.CategoryDto

@Component
class CategoryMapper(
    private val productMapper: ProductMapper,
) {
    fun toDto(entity: CategoryEntity): CategoryDto {
        return CategoryDto(
            id = entity.id!!,
            parentCategory = entity.parent?.id,
            title = entity.title,
            imageUrl = entity.imageUrl,
            products = productMapper.toDto(entity.products),
            children = entity.children.map { toDto(it) },
            sku = entity.sku,
        )
    }

    fun toDto(entities: List<CategoryEntity>) = entities.map {
        toShortDto(it)
    }

    fun toEntity(model: CategoryDto): CategoryEntity {
        return CategoryEntity(
            title = model.title,
            imageUrl = model.imageUrl,
            sku = model.sku,
        )
    }

    private fun toShortDto(entity: CategoryEntity) = CategoryDto(
        id = entity.id!!,
        parentCategory = entity.parent?.id,
        title = entity.title,
        imageUrl = entity.imageUrl,
        sku = entity.sku,
    )
}
