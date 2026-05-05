package ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificateTransactionType
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "gift_certificate_transactions",
    indexes = [
        Index(name = "idx_gift_certificate_tx_certificate_id", columnList = "gift_certificate_id"),
        Index(name = "idx_gift_certificate_tx_order_id", columnList = "order_id"),
    ],
)
class GiftCertificateTransactionEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "gift_certificate_id", nullable = false)
    var giftCertificateId: UUID,

    @Column(name = "order_id", nullable = false)
    var orderId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var type: GiftCertificateTransactionType,

    @Column(name = "amount_minor", nullable = false)
    var amountMinor: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
