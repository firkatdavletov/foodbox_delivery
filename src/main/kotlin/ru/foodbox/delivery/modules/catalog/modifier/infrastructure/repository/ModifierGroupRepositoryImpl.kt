package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity.ModifierGroupEntity
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa.ModifierGroupJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class ModifierGroupRepositoryImpl(
    private val jpaRepository: ModifierGroupJpaRepository,
) : ModifierGroupRepository {

    override fun findAll(): List<ModifierGroup> {
        return jpaRepository.findAll()
            .sortedWith(compareBy<ModifierGroupEntity> { it.sortOrder }.thenBy { it.name })
            .map(::toDomain)
    }

    override fun findAllByIsActive(isActive: Boolean): List<ModifierGroup> {
        return jpaRepository.findAllByIsActiveOrderBySortOrderAscNameAsc(isActive).map(::toDomain)
    }

    override fun findAllByCodes(codes: Collection<String>): List<ModifierGroup> {
        if (codes.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllByCodeIn(codes).map(::toDomain)
    }

    override fun findAllByIds(ids: Collection<UUID>): List<ModifierGroup> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllById(ids).map(::toDomain)
    }

    override fun findById(id: UUID): ModifierGroup? {
        return jpaRepository.findById(id).getOrNull()?.let(::toDomain)
    }

    override fun findByCode(code: String): ModifierGroup? {
        return jpaRepository.findByCode(code)?.let(::toDomain)
    }

    override fun save(group: ModifierGroup): ModifierGroup {
        val existing = jpaRepository.findById(group.id).getOrNull()
        val entity = existing ?: ModifierGroupEntity(
            id = group.id,
            code = group.code,
            name = group.name,
            minSelected = group.minSelected,
            maxSelected = group.maxSelected,
            isRequired = group.isRequired,
            isActive = group.isActive,
            sortOrder = group.sortOrder,
        )

        entity.code = group.code
        entity.name = group.name
        entity.minSelected = group.minSelected
        entity.maxSelected = group.maxSelected
        entity.isRequired = group.isRequired
        entity.isActive = group.isActive
        entity.sortOrder = group.sortOrder
        return toDomain(jpaRepository.save(entity))
    }

    private fun toDomain(entity: ModifierGroupEntity): ModifierGroup {
        return ModifierGroup(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            minSelected = entity.minSelected,
            maxSelected = entity.maxSelected,
            isRequired = entity.isRequired,
            isActive = entity.isActive,
            sortOrder = entity.sortOrder,
        )
    }
}
