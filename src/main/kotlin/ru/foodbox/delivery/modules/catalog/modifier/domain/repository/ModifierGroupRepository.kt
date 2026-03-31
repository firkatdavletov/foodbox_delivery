package ru.foodbox.delivery.modules.catalog.modifier.domain.repository

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import java.util.UUID

interface ModifierGroupRepository {
    fun findAll(): List<ModifierGroup>
    fun findAllByIsActive(isActive: Boolean): List<ModifierGroup>
    fun findAllByCodes(codes: Collection<String>): List<ModifierGroup>
    fun findAllByIds(ids: Collection<UUID>): List<ModifierGroup>
    fun findById(id: UUID): ModifierGroup?
    fun findByCode(code: String): ModifierGroup?
    fun save(group: ModifierGroup): ModifierGroup
}
