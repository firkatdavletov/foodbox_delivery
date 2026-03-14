package ru.foodbox.delivery.modules.catalog.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionValue
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductOptionValueRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductOptionValueEntity
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductOptionValueJpaRepository
import java.util.UUID

@Repository
class CatalogProductOptionValueRepositoryImpl(
    private val jpaRepository: CatalogProductOptionValueJpaRepository,
) : CatalogProductOptionValueRepository {

    override fun findAllByOptionGroupIds(optionGroupIds: Collection<UUID>): List<CatalogProductOptionValue> {
        if (optionGroupIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByOptionGroupIdInOrderBySortOrderAscIdAsc(optionGroupIds).map(::toDomain)
    }

    override fun deleteAllByOptionGroupIds(optionGroupIds: Collection<UUID>) {
        if (optionGroupIds.isEmpty()) {
            return
        }

        jpaRepository.deleteAllByOptionGroupIdIn(optionGroupIds)
        jpaRepository.flush()
    }

    override fun saveAll(optionValues: List<CatalogProductOptionValue>): List<CatalogProductOptionValue> {
        if (optionValues.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.saveAll(
            optionValues.map { optionValue ->
                CatalogProductOptionValueEntity(
                    id = optionValue.id,
                    optionGroupId = optionValue.optionGroupId,
                    code = optionValue.code,
                    title = optionValue.title,
                    sortOrder = optionValue.sortOrder,
                )
            }
        ).map(::toDomain)
    }

    private fun toDomain(entity: CatalogProductOptionValueEntity): CatalogProductOptionValue {
        return CatalogProductOptionValue(
            id = entity.id,
            optionGroupId = entity.optionGroupId,
            code = entity.code,
            title = entity.title,
            sortOrder = entity.sortOrder,
        )
    }
}
