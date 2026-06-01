package ru.foodbox.delivery.modules.promotions.application.command

import java.util.UUID

data class CalculatePromoDiscountCommand(
    val userId: UUID?,
    val grossTotalMinor: Long,
    val currency: String,
    val promoCode: String,
)
