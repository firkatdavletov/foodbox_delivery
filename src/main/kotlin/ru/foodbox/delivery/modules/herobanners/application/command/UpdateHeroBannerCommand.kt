package ru.foodbox.delivery.modules.herobanners.application.command

import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import java.time.Instant

data class UpdateHeroBannerCommand(
    val code: String,
    val storefrontCode: String,
    val placement: BannerPlacement,
    val status: BannerStatus,
    val sortOrder: Int,
    val desktopImageUrl: String,
    val mobileImageUrl: String?,
    val primaryActionUrl: String?,
    val secondaryActionUrl: String?,
    val themeVariant: BannerThemeVariant,
    val textAlignment: BannerTextAlignment,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val translations: List<HeroBannerTranslationCommand>,
)
