package ru.foodbox.delivery.modules.catalog.api.dto

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
)
