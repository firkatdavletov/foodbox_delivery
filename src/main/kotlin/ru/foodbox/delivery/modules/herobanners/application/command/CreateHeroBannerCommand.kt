package ru.foodbox.delivery.modules.herobanners.application.command

import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import java.time.Instant

data class CreateHeroBannerCommand(
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

data class HeroBannerTranslationCommand(
    val locale: String,
    val title: String,
    val subtitle: String?,
    val description: String?,
    val desktopImageAlt: String,
    val mobileImageAlt: String?,
    val primaryActionLabel: String?,
    val secondaryActionLabel: String?,
)
