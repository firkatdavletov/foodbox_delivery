package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity.ModifierOptionEntity
import java.util.UUID

interface ModifierOptionJpaRepository : JpaRepository<ModifierOptionEntity, UUID> {
    fun findAllByGroupIdInOrderBySortOrderAscNameAsc(groupIds: Collection<UUID>): List<ModifierOptionEntity>
    fun findAllByGroupIdOrderBySortOrderAscNameAsc(groupId: UUID): List<ModifierOptionEntity>
    fun findAllByGroupIdAndIsActiveOrderBySortOrderAscNameAsc(groupId: UUID, isActive: Boolean): List<ModifierOptionEntity>
    fun deleteAllByGroupId(groupId: UUID)
}
