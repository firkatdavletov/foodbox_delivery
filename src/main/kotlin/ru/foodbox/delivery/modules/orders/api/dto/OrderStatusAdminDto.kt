package ru.foodbox.delivery.modules.orders.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import ru.foodbox.delivery.common.security.UserRole
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.domain.OrderStatusChangeSourceType
import java.time.Instant
import java.util.UUID

data class OrderStatusSummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val stateType: OrderStateType,
    val color: String?,
    val icon: String?,
    val isFinal: Boolean,
    val isCancellable: Boolean,
    val visibleToCustomer: Boolean,
)

data class OrderStatusResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val stateType: OrderStateType,
    val color: String?,
    val icon: String?,
    val isInitial: Boolean,
    val isFinal: Boolean,
    val isCancellable: Boolean,
    val isActive: Boolean,
    val visibleToCustomer: Boolean,
    val notifyCustomer: Boolean,
    val notifyStaff: Boolean,
    val sortOrder: Int,
)

data class UpsertOrderStatusRequest(
    val id: UUID? = null,
    @field:NotBlank
    val code: String,
    @field:NotBlank
    val name: String,
    val description: String? = null,
    @field:NotNull
    val stateType: OrderStateType,
    val color: String? = null,
    val icon: String? = null,
    val isInitial: Boolean = false,
    val isFinal: Boolean = false,
    val isCancellable: Boolean = false,
    val isActive: Boolean = true,
    val visibleToCustomer: Boolean = true,
    val notifyCustomer: Boolean = false,
    val notifyStaff: Boolean = true,
    @field:PositiveOrZero
    val sortOrder: Int = 0,
)

data class OrderStatusTransitionResponse(
    val id: UUID,
    val fromStatus: OrderStatusSummaryResponse,
    val toStatus: OrderStatusSummaryResponse,
    val requiredRole: UserRole?,
    val isAutomatic: Boolean,
    val guardCode: String?,
    val isActive: Boolean,
)

data class CreateOrderStatusTransitionRequest(
    @field:NotNull
    val fromStatusId: UUID,
    @field:NotNull
    val toStatusId: UUID,
    val requiredRole: UserRole? = null,
    val isAutomatic: Boolean = false,
    val guardCode: String? = null,
    val isActive: Boolean = true,
)

data class OrderStatusHistoryResponse(
    val id: UUID,
    val previousStatus: OrderStatusSummaryResponse?,
    val currentStatus: OrderStatusSummaryResponse,
    val changeSourceType: OrderStatusChangeSourceType,
    val changedByUserId: UUID?,
    val comment: String?,
    val changedAt: Instant,
)
