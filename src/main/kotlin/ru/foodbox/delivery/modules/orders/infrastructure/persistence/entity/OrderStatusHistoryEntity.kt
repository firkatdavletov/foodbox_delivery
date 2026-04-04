package ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.orders.domain.OrderStatusChangeSourceType
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "order_status_history",
    indexes = [
        Index(name = "idx_order_status_history_order_id_changed_at", columnList = "order_id, changed_at"),
        Index(name = "idx_order_status_history_current_status_id", columnList = "current_status_id"),
    ],
)
class OrderStatusHistoryEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: OrderEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_status_id")
    var previousStatus: OrderStatusDefinitionEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_status_id", nullable = false)
    var currentStatus: OrderStatusDefinitionEntity,

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "change_source_type", nullable = false, length = 32)
    var changeSourceType: OrderStatusChangeSourceType,

    @Column(name = "changed_by_user_id")
    var changedByUserId: UUID? = null,

    @Column(columnDefinition = "text")
    var comment: String? = null,

    @Column(name = "changed_at", nullable = false)
    var changedAt: Instant,
)
