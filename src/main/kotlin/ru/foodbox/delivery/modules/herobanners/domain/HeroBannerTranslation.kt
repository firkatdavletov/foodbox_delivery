package ru.foodbox.delivery.modules.herobanners.domain

import java.util.UUID

data class HeroBannerTranslation(
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
