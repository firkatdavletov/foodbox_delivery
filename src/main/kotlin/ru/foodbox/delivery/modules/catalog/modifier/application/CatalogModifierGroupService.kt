package ru.foodbox.delivery.modules.catalog.modifier.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierGroupCommand
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierOptionCommand
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroupWithOptions
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierOptionRepository
import java.util.UUID

@Service
class CatalogModifierGroupService(
    private val modifierGroupRepository: ModifierGroupRepository,
    private val modifierOptionRepository: ModifierOptionRepository,
) {

    fun getAll(isActive: Boolean? = null): List<ModifierGroup> {
        return when (isActive) {
            null -> modifierGroupRepository.findAll()
            else -> modifierGroupRepository.findAllByIsActive(isActive)
        }
    }

    fun getOptions(groupId: UUID, isActive: Boolean? = null): List<ModifierOption> {
        ensureGroupExists(groupId)
        return when (isActive) {
            null -> modifierOptionRepository.findAllByGroupId(groupId)
            else -> modifierOptionRepository.findAllByGroupIdAndIsActive(groupId, isActive)
        }
    }

    fun getOption(groupId: UUID, optionId: UUID): ModifierOption {
        ensureGroupExists(groupId)
        val option = modifierOptionRepository.findById(optionId)
            ?: throw NotFoundException("Modifier option not found: $optionId")
        if (option.groupId != groupId) {
            throw NotFoundException("Modifier option not found: $optionId")
        }
        return option
    }

    @Transactional
    fun upsertGroup(command: UpsertModifierGroupCommand): ModifierGroup {
        val normalizedCode = command.code.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Modifier group code is required")
        val normalizedName = command.name.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Modifier group name is required")

        validateGroup(command)

        val groupId = command.id ?: UUID.randomUUID()
        val duplicateByCode = modifierGroupRepository.findByCode(normalizedCode)
        if (duplicateByCode != null && duplicateByCode.id != groupId) {
            throw IllegalArgumentException("Modifier group code '$normalizedCode' is already used")
        }

        return modifierGroupRepository.save(
            ModifierGroup(
                id = groupId,
                code = normalizedCode,
                name = normalizedName,
                minSelected = command.minSelected,
                maxSelected = command.maxSelected,
                isRequired = command.isRequired,
                isActive = command.isActive,
                sortOrder = command.sortOrder,
            )
        )
    }

    @Transactional
    fun upsertOption(groupId: UUID, optionId: UUID?, command: UpsertModifierOptionCommand): ModifierOption {
        val group = modifierGroupRepository.findById(groupId)
            ?: throw NotFoundException("Modifier group not found: $groupId")

        val normalizedCode = command.code.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("option.code is required")
        val normalizedName = command.name.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("option.name is required")
        val normalizedDescription = command.description?.trim()?.takeIf { it.isNotBlank() }

        validateOption(command, "option")

        val existingOptions = modifierOptionRepository.findAllByGroupId(groupId)
        val targetId = if (optionId == null) {
            UUID.randomUUID()
        } else {
            val existing = modifierOptionRepository.findById(optionId)
                ?: throw NotFoundException("Modifier option not found: $optionId")
            if (existing.groupId != groupId) {
                throw NotFoundException("Modifier option not found: $optionId")
            }
            existing.id
        }

        if (existingOptions.any { it.id != targetId && it.code == normalizedCode }) {
            throw IllegalArgumentException("Modifier option code '$normalizedCode' is already used in group '${group.code}'")
        }

        val option = ModifierOption(
            id = targetId,
            groupId = groupId,
            code = normalizedCode,
            name = normalizedName,
            description = normalizedDescription,
            priceType = command.priceType,
            price = command.price,
            applicationScope = command.applicationScope,
            isDefault = command.isDefault,
            isActive = command.isActive,
            sortOrder = command.sortOrder,
        )

        val mergedOptions = existingOptions.filterNot { it.id == targetId } + option
        validateDefaultCount(group.maxSelected, mergedOptions)

        return modifierOptionRepository.saveAll(listOf(option)).first()
    }

    @Transactional
    fun upsert(command: UpsertModifierGroupCommand): ModifierGroupWithOptions {
        val group = upsertGroup(command)
        validateOptionSet(command.options, group.maxSelected)

        modifierOptionRepository.deleteAllByGroupId(group.id)
        val options = modifierOptionRepository.saveAll(
            command.options.mapIndexed { index, option ->
                val optionCode = option.code.trim().takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("options[$index].code is required")
                val optionName = option.name.trim().takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("options[$index].name is required")
                val normalizedDescription = option.description?.trim()?.takeIf { it.isNotBlank() }
                validateOption(option, "options[$index]")
                ModifierOption(
                    id = UUID.randomUUID(),
                    groupId = group.id,
                    code = optionCode,
                    name = optionName,
                    description = normalizedDescription,
                    priceType = option.priceType,
                    price = option.price,
                    applicationScope = option.applicationScope,
                    isDefault = option.isDefault,
                    isActive = option.isActive,
                    sortOrder = option.sortOrder,
                )
            }
        )

        return ModifierGroupWithOptions(
            group = group,
            options = options.sortedWith(compareBy<ModifierOption> { it.sortOrder }.thenBy { it.name }),
        )
    }

    private fun ensureGroupExists(groupId: UUID) {
        modifierGroupRepository.findById(groupId)
            ?: throw NotFoundException("Modifier group not found: $groupId")
    }

    private fun validateGroup(command: UpsertModifierGroupCommand) {
        require(command.minSelected >= 0) { "minSelected must be non-negative" }
        require(command.maxSelected > 0) { "maxSelected must be greater than zero" }
        require(command.maxSelected >= command.minSelected) { "maxSelected must be greater than or equal to minSelected" }
        if (command.isRequired && command.minSelected == 0) {
            throw IllegalArgumentException("Required modifier group must have minSelected greater than zero")
        }
    }

    private fun validateOptionSet(options: List<UpsertModifierOptionCommand>, maxSelected: Int) {
        val duplicateCodes = options.groupBy { it.code.trim() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .keys
            .sorted()
        if (duplicateCodes.isNotEmpty()) {
            throw IllegalArgumentException("Duplicate modifier option codes: ${duplicateCodes.joinToString(", ")}")
        }

        options.forEachIndexed { index, option ->
            validateOption(option, "options[$index]")
        }

        val defaultCount = options.count { it.isDefault && it.isActive }
        if (defaultCount > maxSelected) {
            throw IllegalArgumentException("Default active options exceed maxSelected")
        }
    }

    private fun validateDefaultCount(maxSelected: Int, options: List<ModifierOption>) {
        val defaultCount = options.count { it.isDefault && it.isActive }
        if (defaultCount > maxSelected) {
            throw IllegalArgumentException("Default active options exceed maxSelected")
        }
    }

    private fun validateOption(option: UpsertModifierOptionCommand, fieldPrefix: String) {
        if (option.price < 0) {
            throw IllegalArgumentException("$fieldPrefix.price must be non-negative")
        }
        if (option.priceType == ModifierPriceType.FREE && option.price != 0L) {
            throw IllegalArgumentException("$fieldPrefix.price must be zero for FREE priceType")
        }
    }
}
