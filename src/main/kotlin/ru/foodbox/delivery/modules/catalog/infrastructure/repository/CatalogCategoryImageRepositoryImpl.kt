package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategoryImage
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryImageRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogCategoryImageEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogCategoryImageJpaRepository
import java.util.UUID

@Repository
class CatalogCategoryImageRepositoryImpl(
    private val jpaRepository: CatalogCategoryImageJpaRepository,
) : CatalogCategoryImageRepository {

    override fun findAllByCategoryIds(categoryIds: Collection<UUID>): List<CatalogCategoryImage> {
        if (categoryIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByCategoryIdInOrderByCategoryIdAscSortOrderAscCreatedAtAsc(categoryIds).map(::toDomain)
    }

    override fun findAllByImageIds(imageIds: Collection<UUID>): List<CatalogCategoryImage> {
        if (imageIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByImageIdIn(imageIds).map(::toDomain)
    }

    override fun saveAll(images: List<CatalogCategoryImage>): List<CatalogCategoryImage> {
        if (images.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.saveAll(images.map(::toEntity)).map(::toDomain)
    }

    override fun deleteAllByCategoryIds(categoryIds: Collection<UUID>) {
        if (categoryIds.isEmpty()) {
            return
        }

        jpaRepository.deleteAllByCategoryIdIn(categoryIds)
        jpaRepository.flush()
    }

    private fun toEntity(image: CatalogCategoryImage): CatalogCategoryImageEntity {
        return CatalogCategoryImageEntity(
            id = image.id,
            categoryId = image.categoryId,
            imageId = image.imageId,
            sortOrder = image.sortOrder,
            createdAt = image.createdAt,
            updatedAt = image.updatedAt,
        )
    }

    private fun toDomain(entity: CatalogCategoryImageEntity): CatalogCategoryImage {
        return CatalogCategoryImage(
            id = entity.id,
            categoryId = entity.categoryId,
            imageId = entity.imageId,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
