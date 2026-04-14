package ru.foodbox.delivery.modules.catalog.application.command

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalog.modifier.application.command.ReplaceProductModifierGroupCommand
import java.util.UUID

data class UpsertProductCommand(
    val id: UUID?,
    val categoryId: UUID,
    val title: String,
    val slug: String?,
    val description: String?,
    val priceMinor: Long,
    val oldPriceMinor: Long?,
    val sku: String?,
    val imageIds: List<UUID> = emptyList(),
    val unit: ProductUnit,
    val countStep: Int,
    val isActive: Boolean,
    val externalId: String? = null,
    val brand: String? = null,
    val sortOrder: Int? = null,
    val replaceProductVariants: Boolean = true,
    val replaceProductModifierGroups: Boolean = true,
    val optionGroups: List<ReplaceProductOptionGroupCommand> = emptyList(),
    val modifierGroups: List<ReplaceProductModifierGroupCommand> = emptyList(),
    val variants: List<ReplaceProductVariantCommand> = emptyList(),
)
