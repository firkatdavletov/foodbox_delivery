package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogCategoryEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogCategoryJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class CatalogCategoryRepositoryImpl(
    private val jpaRepository: CatalogCategoryJpaRepository,
) : CatalogCategoryRepository {

    override fun findAll(activeOnly: Boolean): List<CatalogCategory> {
        val entities = if (activeOnly) {
            jpaRepository.findAllByIsActiveTrueOrderByNameAsc()
        } else {
            jpaRepository.findAll().sortedBy { it.name }
        }

        return entities.map(::toDomain)
    }

    override fun findAllByIsActive(isActive: Boolean): List<CatalogCategory> {
        return jpaRepository.findAllByIsActiveOrderByNameAsc(isActive).map(::toDomain)
    }

    override fun findById(id: UUID): CatalogCategory? {
        val entity = jpaRepository.findById(id).getOrNull() ?: return null
        return toDomain(entity)
    }

    override fun findByExternalId(externalId: String): CatalogCategory? {
        val entity = jpaRepository.findByExternalId(externalId) ?: return null
        return toDomain(entity)
    }

    override fun findBySlug(slug: String): CatalogCategory? {
        val entity = jpaRepository.findBySlug(slug) ?: return null
        return toDomain(entity)
    }

    override fun findAllByExternalIdIn(externalIds: Collection<String>): List<CatalogCategory> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllByExternalIdIn(externalIds).map(::toDomain)
    }

    override fun findAllBySlugIn(slugs: Collection<String>): List<CatalogCategory> {
        if (slugs.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllBySlugIn(slugs).map(::toDomain)
    }

    override fun save(category: CatalogCategory): CatalogCategory {
        val existing = jpaRepository.findById(category.id).getOrNull()
        val entity = existing ?: CatalogCategoryEntity(
            id = category.id,
            externalId = category.externalId,
            name = category.name,
            slug = category.slug,
            parentId = category.parentId,
            description = category.description,
            sortOrder = category.sortOrder,
            isActive = category.isActive,
            createdAt = category.createdAt,
            updatedAt = category.updatedAt,
        )

        entity.externalId = category.externalId
        entity.name = category.name
        entity.slug = category.slug
        entity.parentId = category.parentId
        entity.description = category.description
        entity.sortOrder = category.sortOrder
        entity.isActive = category.isActive
        entity.updatedAt = category.updatedAt

        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    private fun toDomain(entity: CatalogCategoryEntity): CatalogCategory {
        return CatalogCategory(
            id = entity.id,
            name = entity.name,
            slug = entity.slug,
            imageUrls = emptyList(),
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            externalId = entity.externalId,
            parentId = entity.parentId,
            description = entity.description,
            sortOrder = entity.sortOrder,
        )
    }
}
