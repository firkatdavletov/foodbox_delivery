package ru.foodbox.delivery.modules.herobanners.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.herobanners.application.command.ChangeHeroBannerStatusCommand
import ru.foodbox.delivery.modules.herobanners.application.command.CreateHeroBannerCommand
import ru.foodbox.delivery.modules.herobanners.application.command.HeroBannerTranslationCommand
import ru.foodbox.delivery.modules.herobanners.application.command.ReorderHeroBannersCommand
import ru.foodbox.delivery.modules.herobanners.application.command.UpdateHeroBannerCommand
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.HeroBannerTranslation
import ru.foodbox.delivery.modules.herobanners.domain.PageResult
import ru.foodbox.delivery.modules.herobanners.domain.repository.HeroBannerRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class HeroBannerAdminServiceImpl(
    private val repository: HeroBannerRepository,
    private val clock: Clock,
) : HeroBannerAdminService {

    @Transactional(readOnly = true)
    override fun getBannerPage(
        storefrontCode: String?,
        placement: BannerPlacement?,
        status: BannerStatus?,
        search: String?,
        page: Int,
        size: Int,
    ): PageResult<HeroBanner> {
        return repository.findAllAdmin(storefrontCode, placement, status, search, page, size)
    }

    @Transactional(readOnly = true)
    override fun getBannerById(id: UUID): HeroBanner {
        return findBannerOrThrow(id)
    }

    @Transactional
    override fun createBanner(command: CreateHeroBannerCommand): HeroBanner {
        val now = clock.instant()
        validateDateRange(command.startsAt, command.endsAt)
        validateCtaConsistency(command.translations, command.primaryActionUrl, command.secondaryActionUrl)

        val banner = HeroBanner(
            id = UUID.randomUUID(),
            code = command.code,
            storefrontCode = command.storefrontCode,
            placement = command.placement,
            status = command.status,
            sortOrder = command.sortOrder,
            desktopImageUrl = command.desktopImageUrl,
            mobileImageUrl = command.mobileImageUrl,
            primaryActionUrl = command.primaryActionUrl,
            secondaryActionUrl = command.secondaryActionUrl,
            themeVariant = command.themeVariant,
            textAlignment = command.textAlignment,
            startsAt = command.startsAt,
            endsAt = command.endsAt,
            publishedAt = if (command.status == BannerStatus.PUBLISHED) now else null,
            version = 0,
            deletedAt = null,
            createdAt = now,
            updatedAt = now,
            translations = command.translations.map { it.toDomain() },
        )

        if (command.status == BannerStatus.PUBLISHED) {
            validatePublishable(banner)
        }

        return repository.save(banner)
    }

    @Transactional
    override fun updateBanner(id: UUID, command: UpdateHeroBannerCommand): HeroBanner {
        val existing = findBannerOrThrow(id)
        val now = clock.instant()
        validateDateRange(command.startsAt, command.endsAt)
        validateCtaConsistency(command.translations, command.primaryActionUrl, command.secondaryActionUrl)

        val isPublishing = command.status == BannerStatus.PUBLISHED && existing.status != BannerStatus.PUBLISHED
        val existingTranslationsByLocale = existing.translations.associateBy { it.locale }

        val updated = existing.copy(
            code = command.code,
            storefrontCode = command.storefrontCode,
            placement = command.placement,
            status = command.status,
            sortOrder = command.sortOrder,
            desktopImageUrl = command.desktopImageUrl,
            mobileImageUrl = command.mobileImageUrl,
            primaryActionUrl = command.primaryActionUrl,
            secondaryActionUrl = command.secondaryActionUrl,
            themeVariant = command.themeVariant,
            textAlignment = command.textAlignment,
            startsAt = command.startsAt,
            endsAt = command.endsAt,
            publishedAt = if (isPublishing) now else existing.publishedAt,
            updatedAt = now,
            translations = command.translations.map { t ->
                HeroBannerTranslation(
                    id = existingTranslationsByLocale[t.locale]?.id ?: UUID.randomUUID(),
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

        if (command.status == BannerStatus.PUBLISHED) {
            validatePublishable(updated)
        }

        return repository.save(updated)
    }

    @Transactional
    override fun changeBannerStatus(id: UUID, command: ChangeHeroBannerStatusCommand): HeroBanner {
        val existing = findBannerOrThrow(id)
        val now = clock.instant()
        val isPublishing = command.status == BannerStatus.PUBLISHED && existing.status != BannerStatus.PUBLISHED

        val updated = existing.copy(
            status = command.status,
            publishedAt = if (isPublishing) now else existing.publishedAt,
            updatedAt = now,
        )

        if (command.status == BannerStatus.PUBLISHED) {
            validatePublishable(updated)
        }

        return repository.save(updated)
    }

    @Transactional
    override fun reorderBanners(command: ReorderHeroBannersCommand) {
        val ids = command.items.map { it.id }
        val banners = repository.findAllByIds(ids)
        val bannerMap = banners.associateBy { it.id }
        val now = clock.instant()

        val updated = command.items.mapNotNull { item ->
            val banner = bannerMap[item.id] ?: return@mapNotNull null
            banner.copy(sortOrder = item.sortOrder, updatedAt = now)
        }

        repository.saveAll(updated)
    }

    @Transactional
    override fun deleteBanner(id: UUID) {
        val existing = findBannerOrThrow(id)
        if (existing.deletedAt != null) return

        val now = clock.instant()
        val deleted = existing.copy(
            deletedAt = now,
            updatedAt = now,
            translations = emptyList(),
        )
        repository.save(deleted)
    }

    private fun findBannerOrThrow(id: UUID): HeroBanner {
        val banner = repository.findById(id) ?: throw NotFoundException("Hero banner not found: $id")
        if (banner.deletedAt != null) throw NotFoundException("Hero banner not found: $id")
        return banner
    }

    private fun validatePublishable(banner: HeroBanner) {
        require(banner.desktopImageUrl.isNotBlank()) { "Desktop image URL is required for publishing" }
        require(banner.translations.isNotEmpty()) { "At least one translation is required for publishing" }
        banner.translations.forEach { t ->
            require(t.title.isNotBlank()) {
                "Translation title is required for publishing (locale: ${t.locale})"
            }
            require(t.desktopImageAlt.isNotBlank()) {
                "Desktop image alt text is required for publishing (locale: ${t.locale})"
            }
        }
        if (banner.startsAt != null && banner.endsAt != null) {
            require(banner.endsAt > banner.startsAt) { "End date must be after start date" }
        }
    }

    private fun validateDateRange(startsAt: Instant?, endsAt: Instant?) {
        if (startsAt != null && endsAt != null) {
            require(endsAt > startsAt) { "End date must be after start date" }
        }
    }

    private fun validateCtaConsistency(
        translations: List<HeroBannerTranslationCommand>,
        primaryActionUrl: String?,
        secondaryActionUrl: String?,
    ) {
        translations.forEach { t ->
            if (!t.primaryActionLabel.isNullOrBlank()) {
                require(!primaryActionUrl.isNullOrBlank()) {
                    "Primary action URL is required when primary action label is provided (locale: ${t.locale})"
                }
            }
            if (!primaryActionUrl.isNullOrBlank()) {
                require(!t.primaryActionLabel.isNullOrBlank()) {
                    "Primary action label is required when primary action URL is provided (locale: ${t.locale})"
                }
            }
            if (!t.secondaryActionLabel.isNullOrBlank()) {
                require(!secondaryActionUrl.isNullOrBlank()) {
                    "Secondary action URL is required when secondary action label is provided (locale: ${t.locale})"
                }
            }
            if (!secondaryActionUrl.isNullOrBlank()) {
                require(!t.secondaryActionLabel.isNullOrBlank()) {
                    "Secondary action label is required when secondary action URL is provided (locale: ${t.locale})"
                }
            }
        }
    }

    private fun HeroBannerTranslationCommand.toDomain(): HeroBannerTranslation {
        return HeroBannerTranslation(
            id = UUID.randomUUID(),
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
}
