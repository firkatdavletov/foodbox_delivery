package ru.foodbox.delivery.modules.herobanners.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import java.time.Instant

data class UpdateHeroBannerRequest(
    @field:NotBlank
    val code: String,

    @field:NotBlank
    val storefrontCode: String,

    @field:NotNull
    val placement: BannerPlacement,

    @field:NotNull
    val status: BannerStatus,

    @field:NotNull
    val sortOrder: Int,

    @field:NotBlank
    val desktopImageUrl: String,

    val mobileImageUrl: String? = null,
    val primaryActionUrl: String? = null,
    val secondaryActionUrl: String? = null,

    @field:NotNull
    val themeVariant: BannerThemeVariant,

    @field:NotNull
    val textAlignment: BannerTextAlignment,

    val startsAt: Instant? = null,
    val endsAt: Instant? = null,

    @field:Valid
    val translations: List<HeroBannerTranslationRequest> = emptyList(),
)
