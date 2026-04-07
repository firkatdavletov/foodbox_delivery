package ru.foodbox.delivery.modules.herobanners.api.dto

import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import java.time.Instant
import java.util.UUID

data class HeroBannerAdminResponse(
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
    val createdAt: Instant,
    val updatedAt: Instant,
    val translations: List<HeroBannerTranslationResponse>,
)

data class HeroBannerTranslationResponse(
    val id: UUID,
    val locale: String,
    val title: String,
    val subtitle: String?,
    val description: String?,
    val desktopImageAlt: String,
    val mobileImageAlt: String?,
    val primaryActionLabel: String?,
    val secondaryActionLabel: String?,
)

data class HeroBannerAdminPageResponse(
    val content: List<HeroBannerAdminResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
