package ru.foodbox.delivery.modules.catalog.modifier.domain

import java.util.UUID

data class ModifierGroup(
    val id: UUID,
    val code: String,
    val name: String,
    val minSelected: Int,
    val maxSelected: Int,
    val isRequired: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
)

data class ModifierOption(
    val id: UUID,
    val groupId: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val priceType: ModifierPriceType,
    val price: Long,
    val applicationScope: ModifierApplicationScope,
    val isDefault: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
)

data class ProductModifierGroup(
    val id: UUID,
    val productId: UUID,
    val modifierGroupId: UUID,
    val sortOrder: Int,
    val isActive: Boolean,
)

data class ModifierGroupWithOptions(
    val group: ModifierGroup,
    val options: List<ModifierOption>,
)

data class ProductModifierGroupDetails(
    val id: UUID,
    val code: String,
    val name: String,
    val minSelected: Int,
    val maxSelected: Int,
    val isRequired: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
    val options: List<ProductModifierOptionDetails>,
)

data class ProductModifierOptionDetails(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val priceType: ModifierPriceType,
    val price: Long,
    val applicationScope: ModifierApplicationScope,
    val isDefault: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
)
