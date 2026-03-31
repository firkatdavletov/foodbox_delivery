package ru.foodbox.delivery.modules.cart.modifier.application

import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogProductModifiersService
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierGroupDetails
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierOptionDetails
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemModifierCommand
import ru.foodbox.delivery.modules.cart.modifier.domain.CartItemModifier
import java.util.UUID
import kotlin.math.max

@Service
class CartItemModifierResolver(
    private val catalogProductModifiersService: CatalogProductModifiersService,
) {

    fun resolve(productId: UUID, commands: List<AddCartItemModifierCommand>): List<CartItemModifier> {
        val availableGroups = catalogProductModifiersService.getProductModifierGroups(
            productId = productId,
            activeOnly = true,
        )
        if (availableGroups.isEmpty()) {
            if (commands.isNotEmpty()) {
                throw IllegalArgumentException("Product does not support modifiers")
            }
            return emptyList()
        }

        val normalizedCommands = commands
            .groupBy { it.modifierGroupId to it.modifierOptionId }
            .map { (key, entries) ->
                AddCartItemModifierCommand(
                    modifierGroupId = key.first,
                    modifierOptionId = key.second,
                    quantity = entries.sumOf { command ->
                        require(command.quantity > 0) { "Modifier quantity must be greater than zero" }
                        command.quantity
                    },
                )
            }

        val groupsById = availableGroups.associateBy { it.id }
        val selectionsByGroupId = normalizedCommands.groupBy { it.modifierGroupId }

        val requestedUnknownGroupIds = selectionsByGroupId.keys.filterNot(groupsById::containsKey)
        if (requestedUnknownGroupIds.isNotEmpty()) {
            throw IllegalArgumentException("Modifier group(s) are unavailable for product: ${requestedUnknownGroupIds.joinToString(", ")}")
        }

        availableGroups.forEach { group ->
            val selectedCount = selectionsByGroupId[group.id].orEmpty().size
            val effectiveMin = max(group.minSelected, if (group.isRequired) 1 else 0)
            if (selectedCount < effectiveMin) {
                throw IllegalArgumentException("Modifier group '${group.code}' requires at least $effectiveMin option(s)")
            }
            if (selectedCount > group.maxSelected) {
                throw IllegalArgumentException("Modifier group '${group.code}' allows at most ${group.maxSelected} option(s)")
            }
        }

        return normalizedCommands.map { command ->
            val group = groupsById.getValue(command.modifierGroupId)
            val option = group.options.firstOrNull { it.id == command.modifierOptionId }
                ?: throw IllegalArgumentException("Modifier option is not available in group '${group.code}'")
            toSnapshot(group, option, command.quantity)
        }.sortedWith(
            compareBy<CartItemModifier> { modifier ->
                groupsById.getValue(modifier.modifierGroupId).sortOrder
            }.thenBy { modifier ->
                groupsById.getValue(modifier.modifierGroupId).options.first { it.id == modifier.modifierOptionId }.sortOrder
            },
        )
    }

    private fun toSnapshot(
        group: ProductModifierGroupDetails,
        option: ProductModifierOptionDetails,
        quantity: Int,
    ): CartItemModifier {
        return CartItemModifier(
            modifierGroupId = group.id,
            modifierOptionId = option.id,
            groupCodeSnapshot = group.code,
            groupNameSnapshot = group.name,
            optionCodeSnapshot = option.code,
            optionNameSnapshot = option.name,
            applicationScopeSnapshot = option.applicationScope,
            priceSnapshot = when (option.priceType) {
                ModifierPriceType.FREE -> 0L
                ModifierPriceType.FIXED -> option.price
            },
            quantity = quantity,
        )
    }
}
