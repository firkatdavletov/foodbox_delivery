package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariantImage
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantImageRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductVariantImageEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantImageJpaRepository
import java.util.UUID

@Repository
class CatalogProductVariantImageRepositoryImpl(
    private val jpaRepository: CatalogProductVariantImageJpaRepository,
) : CatalogProductVariantImageRepository {

    override fun findAllByVariantIds(variantIds: Collection<UUID>): List<CatalogProductVariantImage> {
        if (variantIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByVariantIdInOrderByVariantIdAscSortOrderAscCreatedAtAsc(variantIds).map(::toDomain)
    }

    override fun findAllByImageIds(imageIds: Collection<UUID>): List<CatalogProductVariantImage> {
        if (imageIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByImageIdIn(imageIds).map(::toDomain)
    }

    override fun saveAll(images: List<CatalogProductVariantImage>): List<CatalogProductVariantImage> {
        if (images.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.saveAll(images.map(::toEntity)).map(::toDomain)
    }

    override fun deleteAllByVariantIds(variantIds: Collection<UUID>) {
        if (variantIds.isEmpty()) {
            return
        }

        jpaRepository.deleteAllByVariantIdIn(variantIds)
        jpaRepository.flush()
    }

    private fun toEntity(image: CatalogProductVariantImage): CatalogProductVariantImageEntity {
        return CatalogProductVariantImageEntity(
            id = image.id,
            variantId = image.variantId,
            imageId = image.imageId,
            sortOrder = image.sortOrder,
            createdAt = image.createdAt,
            updatedAt = image.updatedAt,
        )
    }

    private fun toDomain(entity: CatalogProductVariantImageEntity): CatalogProductVariantImage {
        return CatalogProductVariantImage(
            id = entity.id,
            variantId = entity.variantId,
            imageId = entity.imageId,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
