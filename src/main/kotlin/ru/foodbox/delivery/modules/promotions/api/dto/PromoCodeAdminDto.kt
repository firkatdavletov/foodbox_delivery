package ru.foodbox.delivery.modules.promotions.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import ru.foodbox.delivery.modules.promotions.application.command.UpsertPromoCodeCommand
import ru.foodbox.delivery.modules.promotions.domain.PromoCode
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeDiscountType
import java.time.Instant
import java.util.UUID

data class PromoCodeAdminResponse(
    val id: UUID,
    val code: String,
    val discountType: PromoCodeDiscountType,
    val discountValue: Long,
    val minOrderAmountMinor: Long?,
    val maxDiscountMinor: Long?,
    val currency: String?,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val usageLimitTotal: Int?,
    val usageLimitPerUser: Int?,
    val usedCount: Int,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class UpsertPromoCodeRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotNull
    val discountType: PromoCodeDiscountType,
    @field:NotNull
    @field:Min(1)
    val discountValue: Long,
    @field:Min(0)
    val minOrderAmountMinor: Long? = null,
    @field:Min(0)
    val maxDiscountMinor: Long? = null,
    @field:Size(min = 3, max = 3)
    val currency: String? = null,
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,
    @field:Min(1)
    val usageLimitTotal: Int? = null,
    @field:Min(1)
    val usageLimitPerUser: Int? = null,
    val active: Boolean = true,
)

fun PromoCode.toAdminResponse(): PromoCodeAdminResponse {
    return PromoCodeAdminResponse(
        id = id,
        code = code,
        discountType = discountType,
        discountValue = discountValue,
        minOrderAmountMinor = minOrderAmountMinor,
        maxDiscountMinor = maxDiscountMinor,
        currency = currency,
        startsAt = startsAt,
        endsAt = endsAt,
        usageLimitTotal = usageLimitTotal,
        usageLimitPerUser = usageLimitPerUser,
        usedCount = usedCount,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun UpsertPromoCodeRequest.toCommand(): UpsertPromoCodeCommand {
    return UpsertPromoCodeCommand(
        code = code,
        discountType = discountType,
        discountValue = discountValue,
        minOrderAmountMinor = minOrderAmountMinor,
        maxDiscountMinor = maxDiscountMinor,
        currency = currency,
        startsAt = startsAt,
        endsAt = endsAt,
        usageLimitTotal = usageLimitTotal,
        usageLimitPerUser = usageLimitPerUser,
        active = active,
    )
}
