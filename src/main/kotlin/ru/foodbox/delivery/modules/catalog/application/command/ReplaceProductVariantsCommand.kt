package ru.foodbox.delivery.modules.catalog.application.command

import java.util.UUID

data class ReplaceProductVariantsCommand(
    val optionGroups: List<ReplaceProductOptionGroupCommand> = emptyList(),
    val variants: List<ReplaceProductVariantCommand> = emptyList(),
)

data class ReplaceProductOptionGroupCommand(
    val code: String,
    val title: String,
    val sortOrder: Int = 0,
    val values: List<ReplaceProductOptionValueCommand> = emptyList(),
)

data class ReplaceProductOptionValueCommand(
    val code: String,
    val title: String,
    val sortOrder: Int = 0,
)

data class ReplaceProductVariantCommand(
    val externalId: String? = null,
    val sku: String,
    val title: String? = null,
    val priceMinor: Long? = null,
    val oldPriceMinor: Long? = null,
    val imageIds: List<UUID> = emptyList(),
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val options: List<ReplaceProductVariantOptionCommand> = emptyList(),
)

data class ReplaceProductVariantOptionCommand(
    val optionGroupCode: String,
    val optionValueCode: String,
)
