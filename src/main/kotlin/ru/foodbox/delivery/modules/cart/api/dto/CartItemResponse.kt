package ru.foodbox.delivery.modules.cart.api.dto

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import java.util.UUID

data class CartItemResponse(
    val id: UUID,
    val productId: UUID,
    val variantId: UUID?,
    val title: String,
    val imageUrl: String?,
    val unit: ProductUnit,
    val countStep: Int,
    val quantity: Int,
    val priceMinor: Long,
    val unitPriceMinor: Long,
    val modifiersTotalMinor: Long,
    val lineTotalMinor: Long,
    val modifiers: List<CartItemModifierResponse>,
)

data class CartItemModifierResponse(
    val modifierGroupId: UUID,
    val modifierOptionId: UUID,
    val groupCode: String,
    val groupName: String,
    val optionCode: String,
    val optionName: String,
    val applicationScope: ModifierApplicationScope,
    val priceMinor: Long,
    val quantity: Int,
)
