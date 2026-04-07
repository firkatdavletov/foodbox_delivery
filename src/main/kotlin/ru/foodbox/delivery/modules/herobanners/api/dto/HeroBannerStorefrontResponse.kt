package ru.foodbox.delivery.modules.herobanners.api.dto

import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import java.util.UUID

data class HeroBannerStorefrontResponse(
    val id: UUID,
    val code: String,
    val placement: BannerPlacement,
    val title: String,
    val subtitle: String?,
    val description: String?,
    val desktopImageUrl: String,
    val mobileImageUrl: String,
    val desktopImageAlt: String,
    val mobileImageAlt: String,
    val primaryActionLabel: String?,
    val primaryActionUrl: String?,
    val secondaryActionLabel: String?,
    val secondaryActionUrl: String?,
    val themeVariant: BannerThemeVariant,
    val textAlignment: BannerTextAlignment,
    val sortOrder: Int,
)
