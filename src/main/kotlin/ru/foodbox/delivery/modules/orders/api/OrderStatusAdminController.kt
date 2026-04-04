package ru.foodbox.delivery.modules.orders.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.orders.api.dto.CreateOrderStatusTransitionRequest
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusTransitionResponse
import ru.foodbox.delivery.modules.orders.api.dto.UpsertOrderStatusRequest
import ru.foodbox.delivery.modules.orders.application.OrderStatusAdminService
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
class OrderStatusAdminController(
    private val orderStatusAdminService: OrderStatusAdminService,
) {

    @GetMapping("/order-statuses")
    fun getStatuses(
        @RequestParam(name = "includeInactive", defaultValue = "true") includeInactive: Boolean,
    ): List<OrderStatusResponse> {
        return orderStatusAdminService.getStatuses(includeInactive).map(OrderStatusDefinition::toResponse)
    }

    @GetMapping("/order-statuses/{statusId}")
    fun getStatus(
        @PathVariable statusId: UUID,
    ): OrderStatusResponse {
        return orderStatusAdminService.getStatus(statusId).toResponse()
    }

    @PostMapping("/order-statuses")
    fun createStatus(
        @Valid @RequestBody request: UpsertOrderStatusRequest,
    ): OrderStatusResponse {
        return orderStatusAdminService.saveStatus(request.toDomain()).toResponse()
    }

    @PutMapping("/order-statuses/{statusId}")
    fun updateStatus(
        @PathVariable statusId: UUID,
        @Valid @RequestBody request: UpsertOrderStatusRequest,
    ): OrderStatusResponse {
        require(request.id == null || request.id == statusId) {
            "Order status id in path and payload must match"
        }
        return orderStatusAdminService.saveStatus(request.toDomain(statusId)).toResponse()
    }

    @DeleteMapping("/order-statuses/{statusId}")
    @ResponseStatus(HttpStatus.OK)
    fun deactivateStatus(
        @PathVariable statusId: UUID,
    ): OrderStatusResponse {
        return orderStatusAdminService.deactivateStatus(statusId).toResponse()
    }

    @GetMapping("/order-status-transitions")
    fun getTransitions(
        @RequestParam(name = "statusId", required = false) statusId: UUID?,
    ): List<OrderStatusTransitionResponse> {
        return orderStatusAdminService.getTransitions(statusId).map(OrderStatusTransition::toResponse)
    }

    @PostMapping("/order-status-transitions")
    fun createTransition(
        @Valid @RequestBody request: CreateOrderStatusTransitionRequest,
    ): OrderStatusTransitionResponse {
        return orderStatusAdminService.createTransition(request.toDomain(orderStatusAdminService)).toResponse()
    }

    @DeleteMapping("/order-status-transitions/{transitionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTransition(
        @PathVariable transitionId: UUID,
    ) {
        orderStatusAdminService.deleteTransition(transitionId)
    }
}

private fun UpsertOrderStatusRequest.toDomain(statusId: UUID = id ?: UUID.randomUUID()): OrderStatusDefinition {
    return OrderStatusDefinition(
        id = statusId,
        code = code,
        name = name,
        description = description,
        stateType = stateType,
        color = color,
        icon = icon,
        isInitial = isInitial,
        isFinal = isFinal,
        isCancellable = isCancellable,
        isActive = isActive,
        visibleToCustomer = visibleToCustomer,
        notifyCustomer = notifyCustomer,
        notifyStaff = notifyStaff,
        sortOrder = sortOrder,
    )
}

private fun CreateOrderStatusTransitionRequest.toDomain(
    orderStatusAdminService: OrderStatusAdminService,
): OrderStatusTransition {
    return OrderStatusTransition(
        id = UUID.randomUUID(),
        fromStatus = orderStatusAdminService.getStatus(fromStatusId),
        toStatus = orderStatusAdminService.getStatus(toStatusId),
        requiredRole = requiredRole,
        isAutomatic = isAutomatic,
        guardCode = guardCode?.trim()?.takeIf { it.isNotBlank() },
        isActive = isActive,
    )
}
