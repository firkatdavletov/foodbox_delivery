package ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.entity.CatalogProductOptionValueEntity
import java.util.UUID

interface CatalogProductOptionValueJpaRepository : JpaRepository<CatalogProductOptionValueEntity, UUID> {
    fun findAllByOptionGroupIdInOrderBySortOrderAscIdAsc(optionGroupIds: Collection<UUID>): List<CatalogProductOptionValueEntity>
    fun deleteAllByOptionGroupIdIn(optionGroupIds: Collection<UUID>)
}
