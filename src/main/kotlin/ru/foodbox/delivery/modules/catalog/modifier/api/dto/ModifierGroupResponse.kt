package ru.foodbox.delivery.modules.catalog.modifier.api.dto

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import java.util.UUID

data class ModifierGroupResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val minSelected: Int,
    val maxSelected: Int,
    val isRequired: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
)

data class ModifierOptionResponse(
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
