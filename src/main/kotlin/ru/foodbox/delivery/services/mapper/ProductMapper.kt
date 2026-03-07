package ru.foodbox.delivery.services.mapper

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.services.dto.ProductDto
import ru.foodbox.delivery.services.model.UploadImageStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@Component
class ProductMapper(
    @Value("\${s3.endpoint}") private val s3Endpoint: String,
    @Value("\${s3.bucket}") private val s3Bucket: String,
) {
    fun toDto(entity: ProductEntity) = ProductDto(
        id = entity.id!!,
        categoryId = entity.category.id!!,
        price = entity.price
            .multiply(BigDecimal(100))
            .longValueExact(),
        title = entity.title,
        description = entity.description,
        imageUrl = toPublicUrl(entity.images.firstOrNull { it.status == UploadImageStatus.READY }?.storageKey),
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

    private fun toPublicUrl(storageKey: String?): String? {
        if (storageKey.isNullOrBlank()) return null
        if (storageKey.startsWith("http://") || storageKey.startsWith("https://")) {
            return storageKey
        }
        val endpoint = s3Endpoint.trimEnd('/')
        val bucket = s3Bucket.trim('/')
        val key = storageKey.trimStart('/')
        return "$endpoint/$bucket/$key"
    }
}
