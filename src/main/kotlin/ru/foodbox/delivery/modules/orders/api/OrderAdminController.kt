package ru.foodbox.delivery.modules.orders.api

import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.security.UserRole
import ru.foodbox.delivery.common.security.UserPrincipal
import ru.foodbox.delivery.modules.catalog.application.CatalogImageService
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusHistoryResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusTransitionResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListResponse
import ru.foodbox.delivery.modules.orders.application.AdminOrderListQuery
import ru.foodbox.delivery.modules.orders.application.AdminOrderListScope
import ru.foodbox.delivery.modules.orders.application.AdminOrderListService
import ru.foodbox.delivery.modules.orders.application.AdminOrderListSortBy
import ru.foodbox.delivery.modules.orders.application.AdminOrderListSortDirection
import ru.foodbox.delivery.modules.orders.api.dto.UpdateOrderStatusRequest
import ru.foodbox.delivery.modules.orders.application.OrderService
import ru.foodbox.delivery.modules.orders.application.OrderStatusChangeActor
import ru.foodbox.delivery.modules.orders.application.OrderStatusService
import ru.foodbox.delivery.modules.orders.application.command.ChangeOrderStatusCommand
import ru.foodbox.delivery.modules.orders.application.parseAdminOrderListBoundary
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/orders")
class OrderAdminController(
    private val adminOrderListService: AdminOrderListService,
    private val orderService: OrderService,
    private val orderStatusService: OrderStatusService,
    private val catalogImageService: CatalogImageService,
) {

    @GetMapping
    fun getOrders(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "pageSize", defaultValue = "25") pageSize: Int,
        @RequestParam(name = "search", required = false) search: String?,
        @RequestParam(name = "statusCodes", required = false) statusCodes: List<String>?,
        @RequestParam(name = "deliveryMethods", required = false) deliveryMethods: List<String>?,
        @RequestParam(name = "createdFrom", required = false) createdFrom: String?,
        @RequestParam(name = "createdTo", required = false) createdTo: String?,
        @RequestParam(name = "scope", required = false) scope: String?,
        @RequestParam(name = "sortBy", required = false) sortBy: String?,
        @RequestParam(name = "sortDirection", required = false) sortDirection: String?,
    ): AdminOrderListResponse {
        return adminOrderListService.getOrders(
            AdminOrderListQuery(
                page = page,
                pageSize = pageSize,
                search = search,
                statusCodes = statusCodes.orEmpty()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .map(String::uppercase)
                    .toSet(),
                deliveryMethods = deliveryMethods.orEmpty()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .map(DeliveryMethodType::fromValue)
                    .toSet(),
                createdFrom = parseAdminOrderListBoundary(createdFrom, inclusiveEndOfDay = false),
                createdTo = parseAdminOrderListBoundary(createdTo, inclusiveEndOfDay = true),
                scope = AdminOrderListScope.fromApiValue(scope),
                sortBy = AdminOrderListSortBy.fromApiValue(sortBy),
                sortDirection = AdminOrderListSortDirection.fromApiValue(sortDirection),
            )
        )
    }

    @GetMapping("/search")
    fun getOrderByNumber(
        @RequestParam(name = "orderNumber") orderNumber: String,
    ): OrderResponse {
        return orderService.getAdminOrderByNumber(orderNumber).toResponseWithImages()
    }

    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: UUID,
    ): OrderResponse {
        return orderService.getAdminOrder(orderId).toResponseWithImages()
    }

    @GetMapping("/{orderId}/available-status-transitions")
    fun getAvailableStatusTransitions(
        authentication: Authentication,
        @PathVariable orderId: UUID,
    ): List<OrderStatusTransitionResponse> {
        return orderStatusService.getAvailableTransitions(
            orderId = orderId,
            actor = authentication.toStatusActor(),
        ).map { it.toResponse() }
    }

    @GetMapping("/{orderId}/status-history")
    fun getStatusHistory(
        @PathVariable orderId: UUID,
    ): List<OrderStatusHistoryResponse> {
        return orderStatusService.getStatusHistory(orderId).map { it.toResponse() }
    }

    @PostMapping("/{orderId}/status")
    fun changeStatus(
        authentication: Authentication,
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: UpdateOrderStatusRequest,
    ): OrderResponse {
        return updateStatusInternal(authentication, orderId, request)
    }

    @PatchMapping("/{orderId}/status")
    fun updateStatus(
        authentication: Authentication,
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: UpdateOrderStatusRequest,
    ): OrderResponse {
        return updateStatusInternal(authentication, orderId, request)
    }

    private fun updateStatusInternal(
        authentication: Authentication,
        orderId: UUID,
        request: UpdateOrderStatusRequest,
    ): OrderResponse {
        return orderStatusService.changeStatus(
            orderId = orderId,
            command = ChangeOrderStatusCommand(
                targetStatusId = request.statusId,
                targetStatusCode = request.statusCode ?: request.status,
                comment = request.comment,
            ),
            actor = authentication.toStatusActor(),
        ).toResponseWithImages()
    }

    private fun Order.toResponseWithImages(): OrderResponse {
        val productIds = items.map { it.productId }.distinct()
        val thumbUrls = if (productIds.isNotEmpty()) {
            catalogImageService.getFirstProductThumbUrl(productIds)
        } else {
            emptyMap()
        }
        return toResponse(thumbUrls)
    }

    private fun List<Order>.toResponsesWithImages(): List<OrderResponse> {
        val productIds = flatMap { order -> order.items.map { it.productId } }.distinct()
        val thumbUrls = if (productIds.isNotEmpty()) {
            catalogImageService.getFirstProductThumbUrl(productIds)
        } else {
            emptyMap()
        }
        return map { it.toResponse(thumbUrls) }
    }
}

private fun Authentication.toStatusActor(): OrderStatusChangeActor {
    val principal = principal as? UserPrincipal
    return OrderStatusChangeActor(
        sourceType = ru.foodbox.delivery.modules.orders.domain.OrderStatusChangeSourceType.ADMIN,
        actorId = principal?.userId,
        roles = authorities.mapNotNull { authority ->
            runCatching { UserRole.valueOf(authority.authority.removePrefix("ROLE_")) }.getOrNull()
        }.toSet(),
    )
}
