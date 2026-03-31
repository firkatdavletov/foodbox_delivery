package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity.ModifierGroupEntity
import java.util.UUID

interface ModifierGroupJpaRepository : JpaRepository<ModifierGroupEntity, UUID> {
    fun findAllByIsActiveOrderBySortOrderAscNameAsc(isActive: Boolean): List<ModifierGroupEntity>
    fun findAllByCodeIn(codes: Collection<String>): List<ModifierGroupEntity>
    fun findByCode(code: String): ModifierGroupEntity?
}
