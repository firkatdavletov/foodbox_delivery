package ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "order_status_definitions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_order_status_definitions_code",
            columnNames = ["code"],
        )
    ],
    indexes = [
        Index(name = "idx_order_status_definitions_state_type", columnList = "state_type"),
        Index(name = "idx_order_status_definitions_sort_order", columnList = "sort_order"),
    ],
)
class OrderStatusDefinitionEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(nullable = false, length = 64)
    var code: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "state_type", nullable = false, length = 64)
    var stateType: OrderStateType,

    @Column(length = 32)
    var color: String? = null,

    @Column(length = 64)
    var icon: String? = null,

    @Column(name = "is_initial", nullable = false)
    var isInitial: Boolean,

    @Column(name = "is_final", nullable = false)
    var isFinal: Boolean,

    @Column(name = "is_cancellable", nullable = false)
    var isCancellable: Boolean,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(name = "visible_to_customer", nullable = false)
    var visibleToCustomer: Boolean,

    @Column(name = "notify_customer", nullable = false)
    var notifyCustomer: Boolean,

    @Column(name = "notify_staff", nullable = false)
    var notifyStaff: Boolean,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
