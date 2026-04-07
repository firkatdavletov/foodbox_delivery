package ru.foodbox.delivery.modules.herobanners.domain

import java.time.Instant
import java.util.UUID

data class HeroBanner(
    val id: UUID,
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
    val publishedAt: Instant?,
    val version: Long,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val translations: List<HeroBannerTranslation>,
)
