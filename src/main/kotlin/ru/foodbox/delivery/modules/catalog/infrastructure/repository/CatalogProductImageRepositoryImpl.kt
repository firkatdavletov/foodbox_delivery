package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductImage
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductImageRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductImageEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductImageJpaRepository
import java.util.UUID

@Repository
class CatalogProductImageRepositoryImpl(
    private val jpaRepository: CatalogProductImageJpaRepository,
) : CatalogProductImageRepository {

    override fun findAllByProductIds(productIds: Collection<UUID>): List<CatalogProductImage> {
        if (productIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByProductIdInOrderByProductIdAscSortOrderAscCreatedAtAsc(productIds).map(::toDomain)
    }

    override fun findAllByImageIds(imageIds: Collection<UUID>): List<CatalogProductImage> {
        if (imageIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByImageIdIn(imageIds).map(::toDomain)
    }

    override fun saveAll(images: List<CatalogProductImage>): List<CatalogProductImage> {
        if (images.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.saveAll(images.map(::toEntity)).map(::toDomain)
    }

    override fun deleteAllByProductIds(productIds: Collection<UUID>) {
        if (productIds.isEmpty()) {
            return
        }

        jpaRepository.deleteAllByProductIdIn(productIds)
        jpaRepository.flush()
    }

    private fun toEntity(image: CatalogProductImage): CatalogProductImageEntity {
        return CatalogProductImageEntity(
            id = image.id,
            productId = image.productId,
            imageId = image.imageId,
            sortOrder = image.sortOrder,
            createdAt = image.createdAt,
            updatedAt = image.updatedAt,
        )
    }

    private fun toDomain(entity: CatalogProductImageEntity): CatalogProductImage {
        return CatalogProductImage(
            id = entity.id,
            productId = entity.productId,
            imageId = entity.imageId,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
