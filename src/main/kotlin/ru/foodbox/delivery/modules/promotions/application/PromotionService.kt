package ru.foodbox.delivery.modules.promotions.application

import ru.foodbox.delivery.modules.promotions.application.command.ApplyOrderPromotionsCommand
import ru.foodbox.delivery.modules.promotions.application.command.CalculatePromoDiscountCommand

interface PromotionService {
    fun applyOrderPromotions(command: ApplyOrderPromotionsCommand): OrderPricingAdjustment
    fun calculatePromoDiscount(command: CalculatePromoDiscountCommand): PromoDiscountPreview
}
