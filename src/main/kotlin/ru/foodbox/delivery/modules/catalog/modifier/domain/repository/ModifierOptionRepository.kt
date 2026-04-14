package ru.foodbox.delivery.modules.catalog.modifier.domain.repository

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import java.util.UUID

interface ModifierOptionRepository {
    fun findAllByGroupIds(groupIds: Collection<UUID>): List<ModifierOption>
    fun findAllByGroupId(groupId: UUID): List<ModifierOption>
    fun findAllByGroupIdAndIsActive(groupId: UUID, isActive: Boolean): List<ModifierOption>
    fun findById(id: UUID): ModifierOption?
    fun deleteAllByGroupId(groupId: UUID)
    fun saveAll(options: List<ModifierOption>): List<ModifierOption>
}
