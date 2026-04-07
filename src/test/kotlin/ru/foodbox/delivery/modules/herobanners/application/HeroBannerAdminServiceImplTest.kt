package ru.foodbox.delivery.modules.herobanners.application

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.foodbox.delivery.modules.herobanners.application.command.ChangeHeroBannerStatusCommand
import ru.foodbox.delivery.modules.herobanners.application.command.CreateHeroBannerCommand
import ru.foodbox.delivery.modules.herobanners.application.command.HeroBannerTranslationCommand
import ru.foodbox.delivery.modules.herobanners.application.command.ReorderHeroBannersCommand
import ru.foodbox.delivery.modules.herobanners.application.command.UpdateHeroBannerCommand
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.HeroBannerTranslation
import ru.foodbox.delivery.modules.herobanners.domain.PageResult
import ru.foodbox.delivery.modules.herobanners.domain.repository.HeroBannerRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeroBannerAdminServiceImplTest {

    private val fixedNow = Instant.parse("2026-04-07T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)

    @Test
    fun `createBanner creates a draft banner successfully`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        val banner = service.createBanner(
            createCommand(status = BannerStatus.DRAFT)
        )

        assertEquals(BannerStatus.DRAFT, banner.status)
        assertEquals("hero-1", banner.code)
        assertEquals("default", banner.storefrontCode)
        assertNull(banner.publishedAt)
        assertEquals(1, banner.translations.size)
        assertEquals("ru", banner.translations.first().locale)
    }

    @Test
    fun `createBanner with PUBLISHED sets publishedAt`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        val banner = service.createBanner(
            createCommand(status = BannerStatus.PUBLISHED)
        )

        assertEquals(BannerStatus.PUBLISHED, banner.status)
        assertEquals(fixedNow, banner.publishedAt)
    }

    @Test
    fun `createBanner with PUBLISHED fails when no translations`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        assertThrows<IllegalArgumentException> {
            service.createBanner(
                createCommand(status = BannerStatus.PUBLISHED, translations = emptyList())
            )
        }
    }

    @Test
    fun `createBanner with PUBLISHED fails when translation title is blank`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        assertThrows<IllegalArgumentException> {
            service.createBanner(
                createCommand(
                    status = BannerStatus.PUBLISHED,
                    translations = listOf(translationCommand(title = "   "))
                )
            )
        }
    }

    @Test
    fun `createBanner with PUBLISHED fails when desktop image alt is blank`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        assertThrows<IllegalArgumentException> {
            service.createBanner(
                createCommand(
                    status = BannerStatus.PUBLISHED,
                    translations = listOf(translationCommand(desktopImageAlt = ""))
                )
            )
        }
    }

    @Test
    fun `createBanner fails when endsAt is before startsAt`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        assertThrows<IllegalArgumentException> {
            service.createBanner(
                createCommand(
                    startsAt = fixedNow.plusSeconds(3600),
                    endsAt = fixedNow,
                )
            )
        }
    }

    @Test
    fun `createBanner fails when primary label provided without URL`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        assertThrows<IllegalArgumentException> {
            service.createBanner(
                createCommand(
                    primaryActionUrl = null,
                    translations = listOf(translationCommand(primaryActionLabel = "Click me"))
                )
            )
        }
    }

    @Test
    fun `createBanner fails when primary URL provided without label`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        assertThrows<IllegalArgumentException> {
            service.createBanner(
                createCommand(
                    primaryActionUrl = "https://example.com",
                    translations = listOf(translationCommand(primaryActionLabel = null))
                )
            )
        }
    }

    @Test
    fun `changeBannerStatus from DRAFT to PUBLISHED validates and sets publishedAt`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        val created = service.createBanner(createCommand(status = BannerStatus.DRAFT))
        val published = service.changeBannerStatus(
            created.id,
            ChangeHeroBannerStatusCommand(BannerStatus.PUBLISHED),
        )

        assertEquals(BannerStatus.PUBLISHED, published.status)
        assertEquals(fixedNow, published.publishedAt)
    }

    @Test
    fun `changeBannerStatus to PUBLISHED fails without translations`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        val created = service.createBanner(
            createCommand(status = BannerStatus.DRAFT, translations = emptyList())
        )

        assertThrows<IllegalArgumentException> {
            service.changeBannerStatus(
                created.id,
                ChangeHeroBannerStatusCommand(BannerStatus.PUBLISHED),
            )
        }
    }

    @Test
    fun `updateBanner correctly updates translations without duplicates`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        val created = service.createBanner(
            createCommand(
                translations = listOf(
                    translationCommand(locale = "ru"),
                    translationCommand(locale = "en"),
                )
            )
        )

        assertEquals(2, created.translations.size)
        val ruId = created.translations.first { it.locale == "ru" }.id

        val updated = service.updateBanner(
            created.id,
            UpdateHeroBannerCommand(
                code = "hero-1",
                storefrontCode = "default",
                placement = BannerPlacement.HOME_HERO,
                status = BannerStatus.DRAFT,
                sortOrder = 10,
                desktopImageUrl = "https://cdn.example.com/banner.jpg",
                mobileImageUrl = null,
                primaryActionUrl = null,
                secondaryActionUrl = null,
                themeVariant = BannerThemeVariant.LIGHT,
                textAlignment = BannerTextAlignment.LEFT,
                startsAt = null,
                endsAt = null,
                translations = listOf(
                    translationCommand(locale = "ru", title = "Updated title"),
                    translationCommand(locale = "kk"),
                ),
            ),
        )

        assertEquals(2, updated.translations.size)
        val updatedRu = updated.translations.first { it.locale == "ru" }
        assertEquals(ruId, updatedRu.id)
        assertEquals("Updated title", updatedRu.title)
        assertTrue(updated.translations.any { it.locale == "kk" })
        assertTrue(updated.translations.none { it.locale == "en" })
    }

    @Test
    fun `reorderBanners changes sort order`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        val b1 = service.createBanner(createCommand(code = "b1", sortOrder = 10))
        val b2 = service.createBanner(createCommand(code = "b2", sortOrder = 20))

        service.reorderBanners(
            ReorderHeroBannersCommand(
                items = listOf(
                    ReorderHeroBannersCommand.ReorderItem(b1.id, 30),
                    ReorderHeroBannersCommand.ReorderItem(b2.id, 5),
                )
            )
        )

        val updated1 = service.getBannerById(b1.id)
        val updated2 = service.getBannerById(b2.id)
        assertEquals(30, updated1.sortOrder)
        assertEquals(5, updated2.sortOrder)
    }

    @Test
    fun `deleteBanner soft deletes and clears translations`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        val created = service.createBanner(createCommand())
        service.deleteBanner(created.id)

        val raw = repository.findByIdIncludingDeleted(created.id)
        assertNotNull(raw)
        assertNotNull(raw.deletedAt)
        assertTrue(raw.translations.isEmpty())
    }

    @Test
    fun `deleteBanner hides banner from admin list`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        val created = service.createBanner(createCommand())
        service.deleteBanner(created.id)

        assertThrows<ru.foodbox.delivery.common.error.NotFoundException> {
            service.getBannerById(created.id)
        }
    }

    @Test
    fun `admin list filters by storefront and status and placement`() {
        val repository = InMemoryHeroBannerRepository()
        val service = HeroBannerAdminServiceImpl(repository, clock)

        service.createBanner(createCommand(code = "a1", storefrontCode = "site-a", status = BannerStatus.DRAFT))
        service.createBanner(createCommand(code = "a2", storefrontCode = "site-a", status = BannerStatus.PUBLISHED))
        service.createBanner(createCommand(code = "b1", storefrontCode = "site-b", status = BannerStatus.DRAFT))

        val byStorefront = service.getBannerPage("site-a", null, null, null, 0, 20)
        assertEquals(2, byStorefront.totalElements)

        val byStatus = service.getBannerPage(null, null, BannerStatus.DRAFT, null, 0, 20)
        assertEquals(2, byStatus.totalElements)

        val combined = service.getBannerPage("site-a", BannerPlacement.HOME_HERO, BannerStatus.PUBLISHED, null, 0, 20)
        assertEquals(1, combined.totalElements)
        assertEquals("a2", combined.content.first().code)
    }

    private fun createCommand(
        code: String = "hero-1",
        storefrontCode: String = "default",
        status: BannerStatus = BannerStatus.DRAFT,
        sortOrder: Int = 10,
        primaryActionUrl: String? = null,
        secondaryActionUrl: String? = null,
        startsAt: Instant? = null,
        endsAt: Instant? = null,
        translations: List<HeroBannerTranslationCommand> = listOf(translationCommand()),
    ): CreateHeroBannerCommand {
        return CreateHeroBannerCommand(
            code = code,
            storefrontCode = storefrontCode,
            placement = BannerPlacement.HOME_HERO,
            status = status,
            sortOrder = sortOrder,
            desktopImageUrl = "https://cdn.example.com/banner.jpg",
            mobileImageUrl = null,
            primaryActionUrl = primaryActionUrl,
            secondaryActionUrl = secondaryActionUrl,
            themeVariant = BannerThemeVariant.LIGHT,
            textAlignment = BannerTextAlignment.LEFT,
            startsAt = startsAt,
            endsAt = endsAt,
            translations = translations,
        )
    }

    private fun translationCommand(
        locale: String = "ru",
        title: String = "Banner title",
        desktopImageAlt: String = "Banner image",
        primaryActionLabel: String? = null,
        secondaryActionLabel: String? = null,
    ): HeroBannerTranslationCommand {
        return HeroBannerTranslationCommand(
            locale = locale,
            title = title,
            subtitle = "Subtitle",
            description = "Description",
            desktopImageAlt = desktopImageAlt,
            mobileImageAlt = null,
            primaryActionLabel = primaryActionLabel,
            secondaryActionLabel = secondaryActionLabel,
        )
    }

    class InMemoryHeroBannerRepository : HeroBannerRepository {
        private val store = linkedMapOf<UUID, HeroBanner>()

        override fun save(banner: HeroBanner): HeroBanner {
            store[banner.id] = banner
            return banner
        }

        override fun saveAll(banners: List<HeroBanner>): List<HeroBanner> {
            return banners.map { save(it) }
        }

        override fun findById(id: UUID): HeroBanner? {
            return store[id]?.takeIf { it.deletedAt == null }
        }

        fun findByIdIncludingDeleted(id: UUID): HeroBanner? = store[id]

        override fun findAllByIds(ids: Collection<UUID>): List<HeroBanner> {
            return ids.mapNotNull { store[it] }.filter { it.deletedAt == null }
        }

        override fun findActiveForStorefront(
            storefrontCode: String,
            placement: BannerPlacement,
            now: Instant,
        ): List<HeroBanner> {
            return store.values
                .filter { it.deletedAt == null }
                .filter { it.status == BannerStatus.PUBLISHED }
                .filter { it.storefrontCode == storefrontCode }
                .filter { it.placement == placement }
                .filter { b -> val s = b.startsAt; s == null || s <= now }
                .filter { b -> val e = b.endsAt; e == null || e > now }
                .sortedWith(compareBy(HeroBanner::sortOrder).thenBy(HeroBanner::createdAt))
        }

        override fun findAllAdmin(
            storefrontCode: String?,
            placement: BannerPlacement?,
            status: BannerStatus?,
            search: String?,
            page: Int,
            size: Int,
        ): PageResult<HeroBanner> {
            val filtered = store.values
                .filter { it.deletedAt == null }
                .filter { storefrontCode == null || it.storefrontCode == storefrontCode }
                .filter { placement == null || it.placement == placement }
                .filter { status == null || it.status == status }
                .filter {
                    search == null ||
                        it.code.contains(search, ignoreCase = true) ||
                        it.translations.any { t -> t.title.contains(search, ignoreCase = true) }
                }
                .sortedWith(compareBy(HeroBanner::sortOrder).thenBy(HeroBanner::createdAt))

            val start = page * size
            val content = filtered.drop(start).take(size)
            return PageResult(
                content = content,
                page = page,
                size = size,
                totalElements = filtered.size.toLong(),
                totalPages = if (filtered.isEmpty()) 0 else (filtered.size + size - 1) / size,
            )
        }
    }
}
