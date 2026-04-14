package ru.foodbox.delivery.modules.catalog.modifier.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import java.util.UUID

data class UpsertModifierGroupRequest(
    val id: UUID? = null,

    @field:NotBlank
    val code: String,

    @field:NotBlank
    val name: String,

    @field:Min(0)
    val minSelected: Int,

    @field:Min(1)
    val maxSelected: Int,

    val isRequired: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
)

data class UpsertModifierOptionRequest(
    val id: UUID? = null,

    @field:NotBlank
    val code: String,

    @field:NotBlank
    val name: String,

    val description: String? = null,

    @field:NotNull
    val priceType: ModifierPriceType,

    @field:Min(0)
    val price: Long,

    @field:NotNull
    val applicationScope: ModifierApplicationScope,

    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
)
