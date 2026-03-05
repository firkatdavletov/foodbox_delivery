package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.services.dto.ProductDto
import ru.foodbox.delivery.services.model.UploadImageStatus
import java.math.BigDecimal
import java.time.LocalDateTime

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
        imageUrl = entity.images.firstOrNull { it.status == UploadImageStatus.READY }?.storageKey,
        unit = entity.unit,
        countStep = entity.countStep,
        displayWeight = entity.displayWeight,
        sku = entity.sku,
    )

    fun toDto(entities: List<ProductEntity>) = entities.map {
        toDto(it)
    }

    fun toEntity(model: ProductDto, categoryEntity: CategoryEntity) = ProductEntity(
        title = model.title,
        description = model.description,
        price = model.price.toBigDecimal() / BigDecimal(100),
        unit = model.unit,
        countStep = model.countStep,
        displayWeight = model.displayWeight,
        category = categoryEntity,
        isActive = true,
        sku = model.sku,
    ).apply {
        created = LocalDateTime.now()
        modified = LocalDateTime.now()
    }
}
