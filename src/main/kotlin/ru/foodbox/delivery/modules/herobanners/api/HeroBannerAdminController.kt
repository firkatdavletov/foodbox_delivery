package ru.foodbox.delivery.modules.herobanners.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.herobanners.api.dto.ChangeHeroBannerStatusRequest
import ru.foodbox.delivery.modules.herobanners.api.dto.CreateHeroBannerRequest
import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerAdminPageResponse
import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerAdminResponse
import ru.foodbox.delivery.modules.herobanners.api.dto.HeroBannerTranslationRequest
import ru.foodbox.delivery.modules.herobanners.api.dto.ReorderHeroBannersRequest
import ru.foodbox.delivery.modules.herobanners.api.dto.UpdateHeroBannerRequest
import ru.foodbox.delivery.modules.herobanners.application.HeroBannerAdminService
import ru.foodbox.delivery.modules.herobanners.application.command.ChangeHeroBannerStatusCommand
import ru.foodbox.delivery.modules.herobanners.application.command.CreateHeroBannerCommand
import ru.foodbox.delivery.modules.herobanners.application.command.HeroBannerTranslationCommand
import ru.foodbox.delivery.modules.herobanners.application.command.ReorderHeroBannersCommand
import ru.foodbox.delivery.modules.herobanners.application.command.UpdateHeroBannerCommand
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/hero-banners")
class HeroBannerAdminController(
    private val adminService: HeroBannerAdminService,
) {

    @GetMapping
    fun listBanners(
        @RequestParam(name = "storefrontCode", required = false) storefrontCode: String?,
        @RequestParam(name = "placement", required = false) placement: BannerPlacement?,
        @RequestParam(name = "status", required = false) status: BannerStatus?,
        @RequestParam(name = "search", required = false) search: String?,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ): HeroBannerAdminPageResponse {
        val bannerPage = adminService.getBannerPage(storefrontCode, placement, status, search, page, size)
        val imagesByBannerId = adminService.getBannerImages(bannerPage.content.map { it.id })
        return bannerPage.toAdminPageResponse(imagesByBannerId)
    }

    @GetMapping("/{id}")
    fun getBanner(
        @PathVariable id: UUID,
    ): HeroBannerAdminResponse {
        val banner = adminService.getBannerById(id)
        val images = adminService.getBannerImages(listOf(id))[id].orEmpty()
        return banner.toAdminResponse(images)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createBanner(
        @Valid @RequestBody request: CreateHeroBannerRequest,
    ): HeroBannerAdminResponse {
        val banner = adminService.createBanner(
            CreateHeroBannerCommand(
                code = request.code,
                storefrontCode = request.storefrontCode,
                placement = request.placement,
                status = request.status,
                sortOrder = request.sortOrder,
                desktopImageUrl = request.desktopImageUrl,
                mobileImageUrl = request.mobileImageUrl,
                primaryActionUrl = request.primaryActionUrl,
                secondaryActionUrl = request.secondaryActionUrl,
                themeVariant = request.themeVariant,
                textAlignment = request.textAlignment,
                startsAt = request.startsAt,
                endsAt = request.endsAt,
                translations = request.translations.map { it.toCommand() },
            )
        )
        val images = adminService.getBannerImages(listOf(banner.id))[banner.id].orEmpty()
        return banner.toAdminResponse(images)
    }

    @PutMapping("/{id}")
    fun updateBanner(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateHeroBannerRequest,
    ): HeroBannerAdminResponse {
        val banner = adminService.updateBanner(
            id = id,
            command = UpdateHeroBannerCommand(
                code = request.code,
                storefrontCode = request.storefrontCode,
                placement = request.placement,
                status = request.status,
                sortOrder = request.sortOrder,
                desktopImageUrl = request.desktopImageUrl,
                mobileImageUrl = request.mobileImageUrl,
                primaryActionUrl = request.primaryActionUrl,
                secondaryActionUrl = request.secondaryActionUrl,
                themeVariant = request.themeVariant,
                textAlignment = request.textAlignment,
                startsAt = request.startsAt,
                endsAt = request.endsAt,
                translations = request.translations.map { it.toCommand() },
            ),
        )
        val images = adminService.getBannerImages(listOf(banner.id))[banner.id].orEmpty()
        return banner.toAdminResponse(images)
    }

    @PatchMapping("/{id}/status")
    fun changeBannerStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangeHeroBannerStatusRequest,
    ): HeroBannerAdminResponse {
        val banner = adminService.changeBannerStatus(
            id = id,
            command = ChangeHeroBannerStatusCommand(status = request.status),
        )
        val images = adminService.getBannerImages(listOf(banner.id))[banner.id].orEmpty()
        return banner.toAdminResponse(images)
    }

    @PatchMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reorderBanners(
        @Valid @RequestBody request: ReorderHeroBannersRequest,
    ) {
        adminService.reorderBanners(
            ReorderHeroBannersCommand(
                items = request.items.map {
                    ReorderHeroBannersCommand.ReorderItem(id = it.id, sortOrder = it.sortOrder)
                },
            )
        )
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteBanner(
        @PathVariable id: UUID,
    ) {
        adminService.deleteBanner(id)
    }

    @DeleteMapping("/{bannerId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteBannerImage(
        @PathVariable bannerId: UUID,
        @PathVariable imageId: UUID,
    ) {
        adminService.deleteBannerImage(bannerId, imageId)
    }
}

private fun HeroBannerTranslationRequest.toCommand(): HeroBannerTranslationCommand {
    return HeroBannerTranslationCommand(
        locale = locale,
        title = title,
        subtitle = subtitle,
        description = description,
        desktopImageAlt = desktopImageAlt,
        mobileImageAlt = mobileImageAlt,
        primaryActionLabel = primaryActionLabel,
        secondaryActionLabel = secondaryActionLabel,
    )
}
