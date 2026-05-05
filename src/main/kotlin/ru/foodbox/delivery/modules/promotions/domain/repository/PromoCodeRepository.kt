package ru.foodbox.delivery.modules.promotions.domain.repository

import ru.foodbox.delivery.modules.promotions.domain.PromoCode
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeFilter
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeRedemption
import java.util.UUID

interface PromoCodeRepository {
    fun findAll(filter: PromoCodeFilter = PromoCodeFilter()): List<PromoCode>
    fun findById(id: UUID): PromoCode?
    fun findByCode(code: String): PromoCode?
    fun findByCodeForUpdate(code: String): PromoCode?
    fun save(promoCode: PromoCode): PromoCode
    fun deleteById(id: UUID)
    fun countUserRedemptions(promoCodeId: UUID, userId: UUID): Long
    fun saveRedemption(redemption: PromoCodeRedemption): PromoCodeRedemption
}
