package ru.foodbox.delivery.modules.herobanners.infrastructure.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "hero_banners",
    indexes = [
        Index(name = "idx_hero_banners_storefront_code", columnList = "storefront_code"),
        Index(name = "idx_hero_banners_placement", columnList = "placement"),
        Index(name = "idx_hero_banners_status", columnList = "status"),
        Index(name = "idx_hero_banners_starts_at", columnList = "starts_at"),
        Index(name = "idx_hero_banners_ends_at", columnList = "ends_at"),
        Index(name = "idx_hero_banners_sort_order", columnList = "sort_order"),
        Index(name = "idx_hero_banners_deleted_at", columnList = "deleted_at"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_hero_banners_storefront_code",
            columnNames = ["storefront_code", "code"],
        ),
    ],
)
class HeroBannerEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(nullable = false, length = 128)
    var code: String,

    @Column(name = "storefront_code", nullable = false, length = 128)
    var storefrontCode: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var placement: BannerPlacement,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: BannerStatus,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Column(name = "desktop_image_url", nullable = false, length = 1024)
    var desktopImageUrl: String,

    @Column(name = "mobile_image_url", length = 1024)
    var mobileImageUrl: String? = null,

    @Column(name = "primary_action_url", length = 1024)
    var primaryActionUrl: String? = null,

    @Column(name = "secondary_action_url", length = 1024)
    var secondaryActionUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_variant", nullable = false, length = 32)
    var themeVariant: BannerThemeVariant,

    @Enumerated(EnumType.STRING)
    @Column(name = "text_alignment", nullable = false, length = 32)
    var textAlignment: BannerTextAlignment,

    @Column(name = "starts_at")
    var startsAt: Instant? = null,

    @Column(name = "ends_at")
    var endsAt: Instant? = null,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @OneToMany(
        mappedBy = "banner",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var translations: MutableList<HeroBannerTranslationEntity> = mutableListOf(),
)
