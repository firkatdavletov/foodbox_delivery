package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.services.dto.CategoryDto

@Component
class CategoryMapper {
    fun toDto(entity: CategoryEntity) = CategoryDto(
        id = entity.id,
        parentCategory = entity.parentCategoryId,
        title = entity.title,
        imageUrl = entity.imageUrl,
    )

    fun toDto(entities: List<CategoryEntity>) = entities.map {
        toDto(it)
    }
}