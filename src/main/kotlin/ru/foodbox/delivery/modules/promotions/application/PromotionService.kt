package ru.foodbox.delivery.modules.promotions.application

import ru.foodbox.delivery.modules.promotions.application.command.ApplyOrderPromotionsCommand

interface PromotionService {
    fun applyOrderPromotions(command: ApplyOrderPromotionsCommand): OrderPricingAdjustment
}
