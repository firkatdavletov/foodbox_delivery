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

    override fun findById(id: UUID): CatalogCategory? {
        val entity = jpaRepository.findById(id).getOrNull() ?: return null
        return toDomain(entity)
    }

    override fun save(category: CatalogCategory): CatalogCategory {
        val existing = jpaRepository.findById(category.id).getOrNull()
        val entity = existing ?: CatalogCategoryEntity(
            id = category.id,
            name = category.name,
            slug = category.slug,
            imageUrl = category.imageUrl,
            isActive = category.isActive,
            createdAt = category.createdAt,
            updatedAt = category.updatedAt,
        )

        entity.name = category.name
        entity.slug = category.slug
        entity.imageUrl = category.imageUrl
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
            imageUrl = entity.imageUrl,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
