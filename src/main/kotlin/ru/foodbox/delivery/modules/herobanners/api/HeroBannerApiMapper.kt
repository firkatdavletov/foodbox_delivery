package ru.foodbox.delivery.modules.herobanners.api

import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerAdminPageResponse
import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerAdminResponse
import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerImageResponse
import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerStorefrontResponse
import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerTranslationResponse
import ru.foodbox.delivery.modules.herobanners.application.StorefrontBannerView
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.PageResult
import ru.foodbox.delivery.modules.media.domain.MediaImage
import java.util.UUID

internal fun StorefrontBannerView.toResponse(): HeroBannerStorefrontResponse {
    return HeroBannerStorefrontResponse(
        id = id,
        code = code,
        placement = placement,
        title = title,
        subtitle = subtitle,
        description = description,
        desktopImageUrl = desktopImageUrl,
        mobileImageUrl = mobileImageUrl,
        desktopImageAlt = desktopImageAlt,
        mobileImageAlt = mobileImageAlt,
        primaryActionLabel = primaryActionLabel,
        primaryActionUrl = primaryActionUrl,
        secondaryActionLabel = secondaryActionLabel,
        secondaryActionUrl = secondaryActionUrl,
        themeVariant = themeVariant,
        textAlignment = textAlignment,
        sortOrder = sortOrder,
    )
}

internal fun HeroBanner.toAdminResponse(images: List<MediaImage> = emptyList()): HeroBannerAdminResponse {
    return HeroBannerAdminResponse(
        id = id,
        code = code,
        storefrontCode = storefrontCode,
        placement = placement,
        status = status,
        sortOrder = sortOrder,
        desktopImageUrl = desktopImageUrl,
        mobileImageUrl = mobileImageUrl,
        images = images.mapNotNull { img ->
            val url = img.publicUrl ?: return@mapNotNull null
            HeroBannerImageResponse(id = img.id, url = url)
        },
        primaryActionUrl = primaryActionUrl,
        secondaryActionUrl = secondaryActionUrl,
        themeVariant = themeVariant,
        textAlignment = textAlignment,
        startsAt = startsAt,
        endsAt = endsAt,
        publishedAt = publishedAt,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt,
        translations = translations.map { t ->
            HeroBannerTranslationResponse(
                id = t.id,
                locale = t.locale,
                title = t.title,
                subtitle = t.subtitle,
                description = t.description,
                desktopImageAlt = t.desktopImageAlt,
                mobileImageAlt = t.mobileImageAlt,
                primaryActionLabel = t.primaryActionLabel,
                secondaryActionLabel = t.secondaryActionLabel,
            )
        },
    )
}

internal fun PageResult<HeroBanner>.toAdminPageResponse(imagesByBannerId: Map<UUID, List<MediaImage>> = emptyMap()): HeroBannerAdminPageResponse {
    return HeroBannerAdminPageResponse(
        content = content.map { it.toAdminResponse(imagesByBannerId[it.id].orEmpty()) },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
}
