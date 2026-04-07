package ru.foodbox.delivery.modules.herobanners.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerStorefrontResponse
import ru.foodbox.delivery.modules.herobanners.application.HeroBannerStorefrontService
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement

@RestController
@RequestMapping("/api/v1/public/hero-banners")
class HeroBannerStorefrontController(
    private val storefrontService: HeroBannerStorefrontService,
) {

    @GetMapping
    fun getActiveBanners(
        @RequestParam(name = "storefrontCode") storefrontCode: String,
        @RequestParam(name = "placement", defaultValue = "HOME_HERO") placement: BannerPlacement,
        @RequestParam(name = "locale", required = false) locale: String?,
    ): List<HeroBannerStorefrontResponse> {
        return storefrontService.getActiveBanners(storefrontCode, placement, locale)
            .map { it.toResponse() }
    }
}
