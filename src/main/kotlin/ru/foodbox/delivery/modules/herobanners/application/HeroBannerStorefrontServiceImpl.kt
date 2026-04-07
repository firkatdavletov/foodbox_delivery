package ru.foodbox.delivery.modules.herobanners.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.HeroBannerTranslation
import ru.foodbox.delivery.modules.herobanners.domain.repository.HeroBannerRepository
import java.time.Clock

@Service
class HeroBannerStorefrontServiceImpl(
    private val repository: HeroBannerRepository,
    private val clock: Clock,
) : HeroBannerStorefrontService {

    @Transactional(readOnly = true)
    override fun getActiveBanners(
        storefrontCode: String,
        placement: BannerPlacement,
        locale: String?,
    ): List<StorefrontBannerView> {
        val now = clock.instant()
        val banners = repository.findActiveForStorefront(storefrontCode, placement, now)
        return banners.mapNotNull { banner ->
            resolveTranslation(banner, locale)?.let { translation ->
                toStorefrontView(banner, translation)
            }
        }
    }

    private fun resolveTranslation(banner: HeroBanner, locale: String?): HeroBannerTranslation? {
        if (banner.translations.isEmpty()) return null
        if (locale != null) {
            val exact = banner.translations.firstOrNull { it.locale == locale }
            if (exact != null) return exact
        }
        return banner.translations.sortedBy { it.locale }.first()
    }

    private fun toStorefrontView(banner: HeroBanner, translation: HeroBannerTranslation): StorefrontBannerView {
        return StorefrontBannerView(
            id = banner.id,
            code = banner.code,
            placement = banner.placement,
            title = translation.title,
            subtitle = translation.subtitle,
            description = translation.description,
            desktopImageUrl = banner.desktopImageUrl,
            mobileImageUrl = banner.mobileImageUrl ?: banner.desktopImageUrl,
            desktopImageAlt = translation.desktopImageAlt,
            mobileImageAlt = translation.mobileImageAlt ?: translation.desktopImageAlt,
            primaryActionLabel = translation.primaryActionLabel,
            primaryActionUrl = banner.primaryActionUrl,
            secondaryActionLabel = translation.secondaryActionLabel,
            secondaryActionUrl = banner.secondaryActionUrl,
            themeVariant = banner.themeVariant,
            textAlignment = banner.textAlignment,
            sortOrder = banner.sortOrder,
        )
    }
}
