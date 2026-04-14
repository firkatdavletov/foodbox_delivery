package ru.foodbox.delivery.modules.catalog.application.command

import java.util.UUID

data class UpsertProductOptionGroupCommand(
    val id: UUID? = null,
    val productId: UUID,
    val code: String,
    val title: String,
    val sortOrder: Int = 0,
)

data class UpsertProductOptionValueCommand(
    val id: UUID? = null,
    val productId: UUID,
    val optionGroupId: UUID,
    val code: String,
    val title: String,
    val sortOrder: Int = 0,
)

data class UpsertProductVariantCommand(
    val id: UUID? = null,
    val productId: UUID,
    val externalId: String? = null,
    val sku: String,
    val title: String? = null,
    val priceMinor: Long? = null,
    val oldPriceMinor: Long? = null,
    val imageIds: List<UUID> = emptyList(),
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val optionValueIds: List<UUID> = emptyList(),
)
