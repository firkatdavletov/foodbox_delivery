package ru.foodbox.delivery.modules.herobanners.domain.repository

import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.PageResult
import java.time.Instant
import java.util.UUID

interface HeroBannerRepository {
    fun save(banner: HeroBanner): HeroBanner
    fun saveAll(banners: List<HeroBanner>): List<HeroBanner>
    fun findById(id: UUID): HeroBanner?
    fun findAllByIds(ids: Collection<UUID>): List<HeroBanner>
    fun findActiveForStorefront(
        storefrontCode: String,
        placement: BannerPlacement,
        now: Instant,
    ): List<HeroBanner>
    fun findAllAdmin(
        storefrontCode: String?,
        placement: BannerPlacement?,
        status: BannerStatus?,
        search: String?,
        page: Int,
        size: Int,
    ): PageResult<HeroBanner>
}
