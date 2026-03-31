package ru.foodbox.delivery.modules.catalog.modifier.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierGroupCommand
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

    fun getAll(isActive: Boolean? = null): List<ModifierGroupWithOptions> {
        val groups = when (isActive) {
            null -> modifierGroupRepository.findAll()
            else -> modifierGroupRepository.findAllByIsActive(isActive)
        }
        return enrich(groups)
    }

    @Transactional
    fun upsert(command: UpsertModifierGroupCommand): ModifierGroupWithOptions {
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

        val group = modifierGroupRepository.save(
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

        modifierOptionRepository.deleteAllByGroupId(group.id)
        val options = modifierOptionRepository.saveAll(
            command.options.mapIndexed { index, option ->
                val optionCode = option.code.trim().takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("options[$index].code is required")
                val optionName = option.name.trim().takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("options[$index].name is required")
                validateOption(option, index)
                ModifierOption(
                    id = UUID.randomUUID(),
                    groupId = group.id,
                    code = optionCode,
                    name = optionName,
                    description = option.description?.trim()?.takeIf { it.isNotBlank() },
                    priceType = option.priceType,
                    price = option.price,
                    applicationScope = option.applicationScope,
                    isDefault = option.isDefault,
                    isActive = option.isActive,
                    sortOrder = option.sortOrder,
                )
            }
        )

        return ModifierGroupWithOptions(group = group, options = options.sortedWith(compareBy<ModifierOption> { it.sortOrder }.thenBy { it.name }))
    }

    private fun enrich(groups: List<ModifierGroup>): List<ModifierGroupWithOptions> {
        if (groups.isEmpty()) {
            return emptyList()
        }
        val optionsByGroupId = modifierOptionRepository.findAllByGroupIds(groups.map { it.id }).groupBy { it.groupId }
        return groups.sortedWith(compareBy<ModifierGroup> { it.sortOrder }.thenBy { it.name }).map { group ->
            ModifierGroupWithOptions(
                group = group,
                options = optionsByGroupId[group.id].orEmpty()
                    .sortedWith(compareBy<ModifierOption> { it.sortOrder }.thenBy { it.name }),
            )
        }
    }

    private fun validateGroup(command: UpsertModifierGroupCommand) {
        require(command.minSelected >= 0) { "minSelected must be non-negative" }
        require(command.maxSelected > 0) { "maxSelected must be greater than zero" }
        require(command.maxSelected >= command.minSelected) { "maxSelected must be greater than or equal to minSelected" }
        if (command.isRequired && command.minSelected == 0) {
            throw IllegalArgumentException("Required modifier group must have minSelected greater than zero")
        }

        val duplicateCodes = command.options.groupBy { it.code.trim() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .keys
            .sorted()
        if (duplicateCodes.isNotEmpty()) {
            throw IllegalArgumentException("Duplicate modifier option codes: ${duplicateCodes.joinToString(", ")}")
        }

        val defaultCount = command.options.count { it.isDefault && it.isActive }
        if (defaultCount > command.maxSelected) {
            throw IllegalArgumentException("Default active options exceed maxSelected")
        }
    }

    private fun validateOption(
        option: ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierOptionCommand,
        index: Int,
    ) {
        if (option.price < 0) {
            throw IllegalArgumentException("options[$index].price must be non-negative")
        }
        if (option.priceType == ModifierPriceType.FREE && option.price != 0L) {
            throw IllegalArgumentException("options[$index].price must be zero for FREE priceType")
        }
    }
}
