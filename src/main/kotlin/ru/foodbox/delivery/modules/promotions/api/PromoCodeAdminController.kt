package ru.foodbox.delivery.modules.promotions.api

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.promotions.api.dto.PromoCodeAdminResponse
import ru.foodbox.delivery.modules.promotions.api.dto.UpsertPromoCodeRequest
import ru.foodbox.delivery.modules.promotions.api.dto.toAdminResponse
import ru.foodbox.delivery.modules.promotions.api.dto.toCommand
import ru.foodbox.delivery.modules.promotions.application.PromoCodeAdminService
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeDiscountType
import ru.foodbox.delivery.modules.promotions.domain.PromoCodeFilter
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/promo-codes")
class PromoCodeAdminController(
    private val promoCodeAdminService: PromoCodeAdminService,
) {

    @GetMapping
    fun getPromoCodes(
        @RequestParam(name = "active", required = false) active: Boolean?,
        @RequestParam(name = "discountType", required = false) discountType: PromoCodeDiscountType?,
        @RequestParam(name = "code", required = false) code: String?,
        @RequestParam(name = "validAt", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        validAt: Instant?,
    ): List<PromoCodeAdminResponse> {
        return promoCodeAdminService.getPromoCodes(
            PromoCodeFilter(
                active = active,
                discountType = discountType,
                code = code,
                validAt = validAt,
            )
        ).map { it.toAdminResponse() }
    }

    @GetMapping("/search")
    fun searchPromoCode(
        @RequestParam(name = "code") code: String,
    ): PromoCodeAdminResponse {
        return promoCodeAdminService.searchPromoCode(code).toAdminResponse()
    }

    @GetMapping("/{promoCodeId}")
    fun getPromoCode(
        @PathVariable promoCodeId: UUID,
    ): PromoCodeAdminResponse {
        return promoCodeAdminService.getPromoCode(promoCodeId).toAdminResponse()
    }

    @PostMapping
    fun createPromoCode(
        @Valid @RequestBody request: UpsertPromoCodeRequest,
    ): PromoCodeAdminResponse {
        return promoCodeAdminService.createPromoCode(request.toCommand()).toAdminResponse()
    }

    @PutMapping("/{promoCodeId}")
    fun updatePromoCode(
        @PathVariable promoCodeId: UUID,
        @Valid @RequestBody request: UpsertPromoCodeRequest,
    ): PromoCodeAdminResponse {
        return promoCodeAdminService.updatePromoCode(
            promoCodeId = promoCodeId,
            command = request.toCommand(),
        ).toAdminResponse()
    }

    @DeleteMapping("/{promoCodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePromoCode(
        @PathVariable promoCodeId: UUID,
    ) {
        promoCodeAdminService.deletePromoCode(promoCodeId)
    }
}
