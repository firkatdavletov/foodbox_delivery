package ru.foodbox.delivery.modules.herobanners.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "hero_banner_translations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_hero_banner_translations_banner_locale",
            columnNames = ["banner_id", "locale"],
        ),
    ],
)
class HeroBannerTranslationEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_id", nullable = false)
    var banner: HeroBannerEntity,

    @Column(nullable = false, length = 16)
    var locale: String,

    @Column(nullable = false, length = 512)
    var title: String,

    @Column(length = 512)
    var subtitle: String? = null,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "desktop_image_alt", nullable = false, length = 512)
    var desktopImageAlt: String,

    @Column(name = "mobile_image_alt", length = 512)
    var mobileImageAlt: String? = null,

    @Column(name = "primary_action_label", length = 255)
    var primaryActionLabel: String? = null,

    @Column(name = "secondary_action_label", length = 255)
    var secondaryActionLabel: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
