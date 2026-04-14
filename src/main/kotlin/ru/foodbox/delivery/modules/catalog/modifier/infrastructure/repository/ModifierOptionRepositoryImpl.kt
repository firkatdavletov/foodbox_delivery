package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierOptionRepository
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity.ModifierOptionEntity
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa.ModifierOptionJpaRepository
import kotlin.jvm.optionals.getOrNull

@Repository
class ModifierOptionRepositoryImpl(
    private val jpaRepository: ModifierOptionJpaRepository,
) : ModifierOptionRepository {

    override fun findAllByGroupIds(groupIds: Collection<java.util.UUID>): List<ModifierOption> {
        if (groupIds.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.findAllByGroupIdInOrderBySortOrderAscNameAsc(groupIds).map(::toDomain)
    }

    override fun findAllByGroupId(groupId: java.util.UUID): List<ModifierOption> {
        return jpaRepository.findAllByGroupIdOrderBySortOrderAscNameAsc(groupId).map(::toDomain)
    }

    override fun findAllByGroupIdAndIsActive(groupId: java.util.UUID, isActive: Boolean): List<ModifierOption> {
        return jpaRepository.findAllByGroupIdAndIsActiveOrderBySortOrderAscNameAsc(groupId, isActive).map(::toDomain)
    }

    override fun findById(id: java.util.UUID): ModifierOption? {
        return jpaRepository.findById(id).getOrNull()?.let(::toDomain)
    }

    override fun deleteAllByGroupId(groupId: java.util.UUID) {
        jpaRepository.deleteAllByGroupId(groupId)
    }

    override fun saveAll(options: List<ModifierOption>): List<ModifierOption> {
        if (options.isEmpty()) {
            return emptyList()
        }
        return jpaRepository.saveAll(options.map(::toEntity)).map(::toDomain)
    }

    private fun toEntity(option: ModifierOption): ModifierOptionEntity {
        return ModifierOptionEntity(
            id = option.id,
            groupId = option.groupId,
            code = option.code,
            name = option.name,
            description = option.description,
            priceType = option.priceType,
            price = option.price,
            applicationScope = option.applicationScope,
            isDefault = option.isDefault,
            isActive = option.isActive,
            sortOrder = option.sortOrder,
        )
    }

    private fun toDomain(entity: ModifierOptionEntity): ModifierOption {
        return ModifierOption(
            id = entity.id,
            groupId = entity.groupId,
            code = entity.code,
            name = entity.name,
            description = entity.description,
            priceType = entity.priceType,
            price = entity.price,
            applicationScope = entity.applicationScope,
            isDefault = entity.isDefault,
            isActive = entity.isActive,
            sortOrder = entity.sortOrder,
        )
    }
}
