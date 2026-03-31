package ru.foodbox.delivery.modules.catalog.modifier.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.catalog.modifier.application.command.ReplaceProductModifierGroupCommand
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierGroupDetails
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierOptionDetails
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierOptionRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ProductModifierGroupRepository
import java.util.UUID

@Service
class CatalogProductModifiersService(
    private val productModifierGroupRepository: ProductModifierGroupRepository,
    private val modifierGroupRepository: ModifierGroupRepository,
    private val modifierOptionRepository: ModifierOptionRepository,
) {

    fun getProductModifierGroups(productId: UUID, activeOnly: Boolean): List<ProductModifierGroupDetails> {
        val links = productModifierGroupRepository.findAllByProductId(productId)
            .let { source -> if (activeOnly) source.filter { it.isActive } else source }
        if (links.isEmpty()) {
            return emptyList()
        }

        val groupsById = modifierGroupRepository.findAllByIds(links.map { it.modifierGroupId }).associateBy { it.id }
        val optionsByGroupId = modifierOptionRepository.findAllByGroupIds(groupsById.keys).groupBy { it.groupId }

        return links.mapNotNull { link ->
            val group = groupsById[link.modifierGroupId] ?: return@mapNotNull null
            if (activeOnly && !group.isActive) {
                return@mapNotNull null
            }

            val options = optionsByGroupId[group.id].orEmpty()
                .let { source -> if (activeOnly) source.filter { it.isActive } else source }
                .sortedWith(compareBy<ModifierOption> { it.sortOrder }.thenBy { it.name })

            ProductModifierGroupDetails(
                id = group.id,
                code = group.code,
                name = group.name,
                minSelected = group.minSelected,
                maxSelected = group.maxSelected,
                isRequired = group.isRequired,
                isActive = group.isActive && link.isActive,
                sortOrder = link.sortOrder,
                options = options.map { option ->
                    ProductModifierOptionDetails(
                        id = option.id,
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
                },
            )
        }
    }

    @Transactional
    fun replaceProductModifierGroups(productId: UUID, commands: List<ReplaceProductModifierGroupCommand>) {
        validateLinks(commands)

        val requestedGroupIds = commands.map { it.modifierGroupId }
        val groupsById = modifierGroupRepository.findAllByIds(requestedGroupIds).associateBy { it.id }
        val missingIds = requestedGroupIds.filterNot(groupsById::containsKey).distinct()
        if (missingIds.isNotEmpty()) {
            throw IllegalArgumentException("Modifier group(s) not found: ${missingIds.joinToString(", ")}")
        }

        productModifierGroupRepository.deleteAllByProductId(productId)
        productModifierGroupRepository.saveAll(
            commands.map { command ->
                ProductModifierGroup(
                    id = UUID.randomUUID(),
                    productId = productId,
                    modifierGroupId = command.modifierGroupId,
                    sortOrder = command.sortOrder,
                    isActive = command.isActive,
                )
            }
        )
    }

    private fun validateLinks(commands: List<ReplaceProductModifierGroupCommand>) {
        val duplicateIds = commands.groupBy { it.modifierGroupId }
            .filterValues { it.size > 1 }
            .keys
            .toList()
        if (duplicateIds.isNotEmpty()) {
            throw IllegalArgumentException("Duplicate modifier group link(s): ${duplicateIds.joinToString(", ")}")
        }
    }
}
