package ru.foodbox.delivery.modules.catalog.modifier.domain.repository

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import java.util.UUID

interface ModifierOptionRepository {
    fun findAllByGroupIds(groupIds: Collection<UUID>): List<ModifierOption>
    fun deleteAllByGroupId(groupId: UUID)
    fun saveAll(options: List<ModifierOption>): List<ModifierOption>
}
