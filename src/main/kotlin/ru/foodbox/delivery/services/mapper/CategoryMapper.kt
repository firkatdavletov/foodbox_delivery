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
        )
    }

    fun toDto(entities: List<CategoryEntity>) = entities.map {
        toShortDto(it)
    }

    private fun toShortDto(entity: CategoryEntity) = CategoryDto(
        id = entity.id!!,
        parentCategory = entity.parent?.id,
        title = entity.title,
        imageUrl = entity.imageUrl,
    )
}