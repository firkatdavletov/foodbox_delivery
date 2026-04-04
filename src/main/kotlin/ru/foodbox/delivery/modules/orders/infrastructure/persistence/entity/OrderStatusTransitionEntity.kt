package ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ru.foodbox.delivery.common.security.UserRole
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "order_status_transitions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_order_status_transitions_from_to",
            columnNames = ["from_status_id", "to_status_id"],
        )
    ],
    indexes = [
        Index(name = "idx_order_status_transitions_from_status_id", columnList = "from_status_id"),
        Index(name = "idx_order_status_transitions_to_status_id", columnList = "to_status_id"),
    ],
)
class OrderStatusTransitionEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_status_id", nullable = false)
    var fromStatus: OrderStatusDefinitionEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_status_id", nullable = false)
    var toStatus: OrderStatusDefinitionEntity,

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "required_role", length = 32)
    var requiredRole: UserRole? = null,

    @Column(name = "is_automatic", nullable = false)
    var isAutomatic: Boolean,

    @Column(name = "guard_code", length = 64)
    var guardCode: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
