package ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificateStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "gift_certificates",
    indexes = [
        Index(name = "idx_gift_certificates_code", columnList = "code"),
        Index(name = "idx_gift_certificates_status", columnList = "status"),
    ],
)
class GiftCertificateEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(nullable = false, unique = true, length = 64)
    var code: String,

    @Column(name = "initial_amount_minor", nullable = false)
    var initialAmountMinor: Long,

    @Column(name = "balance_minor", nullable = false)
    var balanceMinor: Long,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: GiftCertificateStatus,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
