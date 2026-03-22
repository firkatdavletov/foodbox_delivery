package ru.foodbox.delivery.modules.catalog.application.command

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
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
    val optionGroups: List<ReplaceProductOptionGroupCommand> = emptyList(),
    val variants: List<ReplaceProductVariantCommand> = emptyList(),
)
