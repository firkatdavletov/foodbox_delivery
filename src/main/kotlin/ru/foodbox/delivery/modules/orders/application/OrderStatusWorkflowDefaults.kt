package ru.foodbox.delivery.modules.orders.application

import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import java.util.UUID

object OrderStatusWorkflowDefaults {
    val pendingStatusId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
    val confirmedStatusId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000102")
    val canceledStatusId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000103")
    val completedStatusId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000104")

    private val pending = OrderStatusDefinition(
        id = pendingStatusId,
        code = "PENDING",
        name = "Pending",
        description = "New order awaiting confirmation",
        stateType = OrderStateType.AWAITING_CONFIRMATION,
        color = "#F59E0B",
        icon = "clock",
        isInitial = true,
        isFinal = false,
        isCancellable = true,
        isActive = true,
        visibleToCustomer = true,
        notifyCustomer = false,
        notifyStaff = true,
        sortOrder = 10,
    )

    private val confirmed = OrderStatusDefinition(
        id = confirmedStatusId,
        code = "CONFIRMED",
        name = "Confirmed",
        description = "Order confirmed and accepted into work",
        stateType = OrderStateType.CONFIRMED,
        color = "#2563EB",
        icon = "check-circle",
        isInitial = false,
        isFinal = false,
        isCancellable = true,
        isActive = true,
        visibleToCustomer = true,
        notifyCustomer = true,
        notifyStaff = true,
        sortOrder = 20,
    )

    private val canceled = OrderStatusDefinition(
        id = canceledStatusId,
        code = "CANCELLED",
        name = "Cancelled",
        description = "Order was cancelled",
        stateType = OrderStateType.CANCELED,
        color = "#DC2626",
        icon = "x-circle",
        isInitial = false,
        isFinal = true,
        isCancellable = false,
        isActive = true,
        visibleToCustomer = true,
        notifyCustomer = true,
        notifyStaff = true,
        sortOrder = 90,
    )

    private val completed = OrderStatusDefinition(
        id = completedStatusId,
        code = "COMPLETED",
        name = "Completed",
        description = "Order completed successfully",
        stateType = OrderStateType.COMPLETED,
        color = "#16A34A",
        icon = "check-badge",
        isInitial = false,
        isFinal = true,
        isCancellable = false,
        isActive = true,
        visibleToCustomer = true,
        notifyCustomer = true,
        notifyStaff = true,
        sortOrder = 100,
    )

    val statuses: List<OrderStatusDefinition> = listOf(
        pending,
        confirmed,
        canceled,
        completed,
    )

    val transitions: List<OrderStatusTransition> = listOf(
        OrderStatusTransition(
            id = UUID.fromString("00000000-0000-0000-0000-000000000201"),
            fromStatus = pending,
            toStatus = confirmed,
            requiredRole = null,
            isAutomatic = true,
            guardCode = null,
            isActive = true,
        ),
        OrderStatusTransition(
            id = UUID.fromString("00000000-0000-0000-0000-000000000202"),
            fromStatus = pending,
            toStatus = canceled,
            requiredRole = null,
            isAutomatic = false,
            guardCode = null,
            isActive = true,
        ),
        OrderStatusTransition(
            id = UUID.fromString("00000000-0000-0000-0000-000000000203"),
            fromStatus = confirmed,
            toStatus = completed,
            requiredRole = null,
            isAutomatic = false,
            guardCode = null,
            isActive = true,
        ),
        OrderStatusTransition(
            id = UUID.fromString("00000000-0000-0000-0000-000000000204"),
            fromStatus = confirmed,
            toStatus = canceled,
            requiredRole = null,
            isAutomatic = false,
            guardCode = null,
            isActive = true,
        ),
    )
}
