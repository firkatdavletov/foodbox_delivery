package ru.foodbox.delivery.modules.herobanners.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.HeroBannerTranslation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeroBannerStorefrontServiceImplTest {

    private val fixedNow = Instant.parse("2026-04-07T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)

    @Test
    fun `returns only PUBLISHED and time-active banners`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(banner(code = "draft", status = BannerStatus.DRAFT, sortOrder = 1))
        repository.save(banner(code = "published", status = BannerStatus.PUBLISHED, sortOrder = 2))
        repository.save(banner(code = "archived", status = BannerStatus.ARCHIVED, sortOrder = 3))
        repository.save(
            banner(
                code = "future",
                status = BannerStatus.PUBLISHED,
                sortOrder = 4,
                startsAt = fixedNow.plusSeconds(3600),
            )
        )
        repository.save(
            banner(
                code = "expired",
                status = BannerStatus.PUBLISHED,
                sortOrder = 5,
                endsAt = fixedNow.minusSeconds(3600),
            )
        )
        repository.save(
            banner(
                code = "active-window",
                status = BannerStatus.PUBLISHED,
                sortOrder = 6,
                startsAt = fixedNow.minusSeconds(3600),
                endsAt = fixedNow.plusSeconds(3600),
            )
        )

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, "ru")

        assertEquals(2, result.size)
        assertEquals("published", result[0].code)
        assertEquals("active-window", result[1].code)
    }

    @Test
    fun `sorts by sortOrder ASC`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(banner(code = "third", sortOrder = 30))
        repository.save(banner(code = "first", sortOrder = 10))
        repository.save(banner(code = "second", sortOrder = 20))

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, "ru")

        assertEquals(3, result.size)
        assertEquals("first", result[0].code)
        assertEquals("second", result[1].code)
        assertEquals("third", result[2].code)
    }

    @Test
    fun `uses exact locale when available`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(
            banner(
                code = "multi-lang",
                translations = listOf(
                    translation(locale = "en", title = "English Title"),
                    translation(locale = "ru", title = "Russian Title"),
                )
            )
        )

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, "en")

        assertEquals(1, result.size)
        assertEquals("English Title", result[0].title)
    }

    @Test
    fun `falls back to first translation by locale when exact locale not found`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(
            banner(
                code = "no-kk",
                translations = listOf(
                    translation(locale = "ru", title = "Russian"),
                    translation(locale = "en", title = "English"),
                )
            )
        )

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, "kk")

        assertEquals(1, result.size)
        assertEquals("English", result[0].title)
    }

    @Test
    fun `falls back to first translation when locale not provided`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(
            banner(
                code = "no-locale",
                translations = listOf(
                    translation(locale = "ru", title = "Russian"),
                    translation(locale = "en", title = "English"),
                )
            )
        )

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, null)

        assertEquals(1, result.size)
        assertEquals("English", result[0].title)
    }

    @Test
    fun `applies mobile image URL fallback from desktop`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(
            banner(
                code = "no-mobile",
                desktopImageUrl = "https://cdn.example.com/desktop.jpg",
                mobileImageUrl = null,
            )
        )

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, "ru")

        assertEquals(1, result.size)
        assertEquals("https://cdn.example.com/desktop.jpg", result[0].mobileImageUrl)
    }

    @Test
    fun `applies mobile image alt fallback from desktop alt`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(
            banner(
                code = "no-mobile-alt",
                translations = listOf(
                    translation(
                        locale = "ru",
                        desktopImageAlt = "Desktop alt",
                        mobileImageAlt = null,
                    )
                )
            )
        )

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, "ru")

        assertEquals(1, result.size)
        assertEquals("Desktop alt", result[0].mobileImageAlt)
    }

    @Test
    fun `skips banners with no translations`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(banner(code = "no-translations", translations = emptyList()))
        repository.save(banner(code = "with-translations"))

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, "ru")

        assertEquals(1, result.size)
        assertEquals("with-translations", result[0].code)
    }

    @Test
    fun `deleted banners not returned in storefront`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(banner(code = "active"))
        repository.save(banner(code = "deleted", deletedAt = fixedNow))

        val result = service.getActiveBanners("default", BannerPlacement.HOME_HERO, "ru")

        assertEquals(1, result.size)
        assertEquals("active", result[0].code)
    }

    @Test
    fun `filters by storefrontCode`() {
        val repository = HeroBannerAdminServiceImplTest.InMemoryHeroBannerRepository()
        val service = HeroBannerStorefrontServiceImpl(repository, clock)

        repository.save(banner(code = "site-a", storefrontCode = "a"))
        repository.save(banner(code = "site-b", storefrontCode = "b"))

        val result = service.getActiveBanners("a", BannerPlacement.HOME_HERO, "ru")

        assertEquals(1, result.size)
        assertEquals("site-a", result[0].code)
    }

    private fun banner(
        code: String = "hero-1",
        status: BannerStatus = BannerStatus.PUBLISHED,
        sortOrder: Int = 10,
        storefrontCode: String = "default",
        desktopImageUrl: String = "https://cdn.example.com/banner.jpg",
        mobileImageUrl: String? = null,
        startsAt: Instant? = null,
        endsAt: Instant? = null,
        deletedAt: Instant? = null,
        translations: List<HeroBannerTranslation> = listOf(translation()),
    ): HeroBanner {
        return HeroBanner(
            id = UUID.randomUUID(),
            code = code,
            storefrontCode = storefrontCode,
            placement = BannerPlacement.HOME_HERO,
            status = status,
            sortOrder = sortOrder,
            desktopImageUrl = desktopImageUrl,
            mobileImageUrl = mobileImageUrl,
            primaryActionUrl = null,
            secondaryActionUrl = null,
            themeVariant = BannerThemeVariant.LIGHT,
            textAlignment = BannerTextAlignment.LEFT,
            startsAt = startsAt,
            endsAt = endsAt,
            publishedAt = if (status == BannerStatus.PUBLISHED) fixedNow else null,
            version = 0,
            deletedAt = deletedAt,
            createdAt = fixedNow,
            updatedAt = fixedNow,
            translations = translations,
        )
    }

    private fun translation(
        locale: String = "ru",
        title: String = "Banner Title",
        desktopImageAlt: String = "Banner Image",
        mobileImageAlt: String? = null,
    ): HeroBannerTranslation {
        return HeroBannerTranslation(
            id = UUID.randomUUID(),
            locale = locale,
            title = title,
            subtitle = "Subtitle",
            description = "Description",
            desktopImageAlt = desktopImageAlt,
            mobileImageAlt = mobileImageAlt,
            primaryActionLabel = null,
            secondaryActionLabel = null,
        )
    }
}
