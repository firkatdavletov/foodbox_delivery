package ru.foodbox.delivery.modules.herobanners.application

import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement

interface HeroBannerStorefrontService {
    fun getActiveBanners(
        storefrontCode: String,
        placement: BannerPlacement,
        locale: String?,
    ): List<StorefrontBannerView>
}
