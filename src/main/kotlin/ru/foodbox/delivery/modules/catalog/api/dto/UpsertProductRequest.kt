package ru.foodbox.delivery.modules.catalog.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.util.UUID

data class UpsertProductRequest(
    val id: UUID? = null,

    @field:NotNull
    val categoryId: UUID,

    @field:NotBlank
    val title: String,

    val slug: String? = null,
    val description: String? = null,

    @field:Min(0)
    val priceMinor: Long,

    @field:Min(0)
    val oldPriceMinor: Long? = null,

    val sku: String? = null,
    val imageIds: List<UUID> = emptyList(),

    @field:NotNull
    val unit: ProductUnit,

    @field:Min(1)
    val countStep: Int,

    val isActive: Boolean = true,

    @field:Valid
    val modifierGroups: List<UpsertProductModifierGroupLinkRequest>? = null,
)

data class UpsertProductOptionGroupRequest(
    val id: UUID? = null,

    @field:NotBlank
    val code: String,

    @field:NotBlank
    val title: String,
    val sortOrder: Int = 0,
)

data class UpsertProductOptionValueRequest(
    val id: UUID? = null,

    @field:NotBlank
    val code: String,

    @field:NotBlank
    val title: String,

    val sortOrder: Int = 0,
)

data class UpsertProductVariantRequest(
    val id: UUID? = null,
    val externalId: String? = null,

    @field:NotBlank
    val sku: String,

    val title: String? = null,

    @field:Min(0)
    val priceMinor: Long? = null,

    @field:Min(0)
    val oldPriceMinor: Long? = null,

    val imageIds: List<UUID> = emptyList(),
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val optionValueIds: List<UUID> = emptyList(),
)

data class UpsertProductModifierGroupLinkRequest(
    @field:NotNull
    val modifierGroupId: UUID,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
)
