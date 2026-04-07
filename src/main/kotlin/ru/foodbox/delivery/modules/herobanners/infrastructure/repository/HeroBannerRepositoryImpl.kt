package ru.foodbox.delivery.modules.herobanners.infrastructure.repository

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.HeroBannerTranslation
import ru.foodbox.delivery.modules.herobanners.domain.PageResult
import ru.foodbox.delivery.modules.herobanners.domain.repository.HeroBannerRepository
import ru.foodbox.delivery.modules.herobanners.infrastructure.persistence.entity.HeroBannerEntity
import ru.foodbox.delivery.modules.herobanners.infrastructure.persistence.entity.HeroBannerTranslationEntity
import ru.foodbox.delivery.modules.herobanners.infrastructure.persistence.jpa.HeroBannerJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class HeroBannerRepositoryImpl(
    private val jpaRepository: HeroBannerJpaRepository,
) : HeroBannerRepository {

    @Transactional
    override fun save(banner: HeroBanner): HeroBanner {
        val existing = jpaRepository.findById(banner.id).getOrNull()
        val entity = existing ?: HeroBannerEntity(
            id = banner.id,
            code = banner.code,
            storefrontCode = banner.storefrontCode,
            placement = banner.placement,
            status = banner.status,
            sortOrder = banner.sortOrder,
            desktopImageUrl = banner.desktopImageUrl,
            mobileImageUrl = banner.mobileImageUrl,
            primaryActionUrl = banner.primaryActionUrl,
            secondaryActionUrl = banner.secondaryActionUrl,
            themeVariant = banner.themeVariant,
            textAlignment = banner.textAlignment,
            startsAt = banner.startsAt,
            endsAt = banner.endsAt,
            publishedAt = banner.publishedAt,
            deletedAt = banner.deletedAt,
            createdAt = banner.createdAt,
            updatedAt = banner.updatedAt,
        )

        entity.code = banner.code
        entity.storefrontCode = banner.storefrontCode
        entity.placement = banner.placement
        entity.status = banner.status
        entity.sortOrder = banner.sortOrder
        entity.desktopImageUrl = banner.desktopImageUrl
        entity.mobileImageUrl = banner.mobileImageUrl
        entity.primaryActionUrl = banner.primaryActionUrl
        entity.secondaryActionUrl = banner.secondaryActionUrl
        entity.themeVariant = banner.themeVariant
        entity.textAlignment = banner.textAlignment
        entity.startsAt = banner.startsAt
        entity.endsAt = banner.endsAt
        entity.publishedAt = banner.publishedAt
        entity.deletedAt = banner.deletedAt
        entity.updatedAt = banner.updatedAt

        syncTranslations(entity, banner.translations, banner.updatedAt)

        return toDomain(jpaRepository.save(entity))
    }

    @Transactional
    override fun saveAll(banners: List<HeroBanner>): List<HeroBanner> {
        return banners.map { save(it) }
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): HeroBanner? {
        return jpaRepository.findById(id).getOrNull()?.let(::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findAllByIds(ids: Collection<UUID>): List<HeroBanner> {
        if (ids.isEmpty()) return emptyList()
        return jpaRepository.findAllByIdIn(ids).map(::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findActiveForStorefront(
        storefrontCode: String,
        placement: BannerPlacement,
        now: Instant,
    ): List<HeroBanner> {
        return jpaRepository.findActiveForStorefront(
            storefrontCode = storefrontCode,
            placement = placement,
            status = BannerStatus.PUBLISHED,
            now = now,
        ).map(::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findAllAdmin(
        storefrontCode: String?,
        placement: BannerPlacement?,
        status: BannerStatus?,
        search: String?,
        page: Int,
        size: Int,
    ): PageResult<HeroBanner> {
        val pageable = PageRequest.of(page, size, Sort.unsorted())
        val result = jpaRepository.findAllAdmin(
            storefrontCode = storefrontCode,
            placement = placement,
            status = status,
            search = search,
            pageable = pageable,
        )
        return PageResult(
            content = result.content.map(::toDomain),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    private fun syncTranslations(
        entity: HeroBannerEntity,
        translations: List<HeroBannerTranslation>,
        now: Instant,
    ) {
        val existingByLocale = entity.translations.associateBy { it.locale }
        val newLocales = translations.map { it.locale }.toSet()

        entity.translations.removeIf { it.locale !in newLocales }

        for (t in translations) {
            val existingTr = existingByLocale[t.locale]
            if (existingTr != null) {
                existingTr.title = t.title
                existingTr.subtitle = t.subtitle
                existingTr.description = t.description
                existingTr.desktopImageAlt = t.desktopImageAlt
                existingTr.mobileImageAlt = t.mobileImageAlt
                existingTr.primaryActionLabel = t.primaryActionLabel
                existingTr.secondaryActionLabel = t.secondaryActionLabel
                existingTr.updatedAt = now
            } else {
                entity.translations.add(
                    HeroBannerTranslationEntity(
                        id = t.id,
                        banner = entity,
                        locale = t.locale,
                        title = t.title,
                        subtitle = t.subtitle,
                        description = t.description,
                        desktopImageAlt = t.desktopImageAlt,
                        mobileImageAlt = t.mobileImageAlt,
                        primaryActionLabel = t.primaryActionLabel,
                        secondaryActionLabel = t.secondaryActionLabel,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }
        }
    }

    private fun toDomain(entity: HeroBannerEntity): HeroBanner {
        return HeroBanner(
            id = entity.id,
            code = entity.code,
            storefrontCode = entity.storefrontCode,
            placement = entity.placement,
            status = entity.status,
            sortOrder = entity.sortOrder,
            desktopImageUrl = entity.desktopImageUrl,
            mobileImageUrl = entity.mobileImageUrl,
            primaryActionUrl = entity.primaryActionUrl,
            secondaryActionUrl = entity.secondaryActionUrl,
            themeVariant = entity.themeVariant,
            textAlignment = entity.textAlignment,
            startsAt = entity.startsAt,
            endsAt = entity.endsAt,
            publishedAt = entity.publishedAt,
            version = entity.version,
            deletedAt = entity.deletedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            translations = entity.translations.map { t ->
                HeroBannerTranslation(
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
}
