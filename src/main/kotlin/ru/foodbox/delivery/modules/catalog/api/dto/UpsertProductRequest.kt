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
    val imageUrl: String? = null,

    @field:NotNull
    val unit: ProductUnit,

    @field:Min(1)
    val countStep: Int,

    val isActive: Boolean = true,

    @field:Valid
    val optionGroups: List<UpsertProductOptionGroupRequest> = emptyList(),

    @field:Valid
    val variants: List<UpsertProductVariantRequest> = emptyList(),
)

data class UpsertProductOptionGroupRequest(
    @field:NotBlank
    val code: String,

    @field:NotBlank
    val title: String,

    val sortOrder: Int = 0,

    @field:Valid
    val values: List<UpsertProductOptionValueRequest> = emptyList(),
)

data class UpsertProductOptionValueRequest(
    @field:NotBlank
    val code: String,

    @field:NotBlank
    val title: String,

    val sortOrder: Int = 0,
)

data class UpsertProductVariantRequest(
    val externalId: String? = null,

    @field:NotBlank
    val sku: String,

    val title: String? = null,

    @field:Min(0)
    val priceMinor: Long? = null,

    @field:Min(0)
    val oldPriceMinor: Long? = null,

    val imageUrl: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,

    @field:Valid
    val options: List<UpsertProductVariantOptionRequest> = emptyList(),
)

data class UpsertProductVariantOptionRequest(
    @field:NotBlank
    val optionGroupCode: String,

    @field:NotBlank
    val optionValueCode: String,
)
