package ru.foodbox.delivery.modules.herobanners.application

import ru.foodbox.delivery.modules.herobanners.application.command.ChangeHeroBannerStatusCommand
import ru.foodbox.delivery.modules.herobanners.application.command.CreateHeroBannerCommand
import ru.foodbox.delivery.modules.herobanners.application.command.ReorderHeroBannersCommand
import ru.foodbox.delivery.modules.herobanners.application.command.UpdateHeroBannerCommand
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.PageResult
import java.util.UUID

interface HeroBannerAdminService {
    fun getBannerPage(
        storefrontCode: String?,
        placement: BannerPlacement?,
        status: BannerStatus?,
        search: String?,
        page: Int,
        size: Int,
    ): PageResult<HeroBanner>

    fun getBannerById(id: UUID): HeroBanner
    fun createBanner(command: CreateHeroBannerCommand): HeroBanner
    fun updateBanner(id: UUID, command: UpdateHeroBannerCommand): HeroBanner
    fun changeBannerStatus(id: UUID, command: ChangeHeroBannerStatusCommand): HeroBanner
    fun reorderBanners(command: ReorderHeroBannersCommand)
    fun deleteBanner(id: UUID)
}
