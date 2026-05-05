package ru.foodbox.delivery.modules.orders.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.common.web.CurrentActorParam
import ru.foodbox.delivery.modules.catalog.application.CatalogImageService
import ru.foodbox.delivery.modules.delivery.api.dto.toDomain
import ru.foodbox.delivery.modules.orders.api.dto.CancelOrderRequest
import ru.foodbox.delivery.modules.orders.api.dto.CheckoutRequest
import ru.foodbox.delivery.modules.orders.api.dto.GuestCheckoutRequest
import ru.foodbox.delivery.modules.orders.api.dto.OrderResponse
import ru.foodbox.delivery.modules.orders.application.OrderService
import ru.foodbox.delivery.modules.orders.application.command.CheckoutCommand
import ru.foodbox.delivery.modules.orders.application.command.GuestCheckoutCommand
import ru.foodbox.delivery.modules.orders.application.command.GuestCheckoutItemCommand
import ru.foodbox.delivery.modules.orders.domain.Order
import java.util.UUID

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
    private val catalogImageService: CatalogImageService,
) {

    @PostMapping("/checkout")
    fun checkout(
        @CurrentActorParam actor: CurrentActor,
        @Valid @RequestBody request: CheckoutRequest,
    ): OrderResponse {
        return orderService.checkout(
            actor = actor,
            command = CheckoutCommand(
                paymentMethodCode = request.paymentMethodCode,
                customerName = request.customerName,
                customerPhone = request.customerPhone,
                customerEmail = request.customerEmail,
                deliveryAddress = request.address?.toDomain(),
                comment = request.comment,
                promoCode = request.promoCode,
                giftCertificateCode = request.giftCertificateCode,
            ),
        ).toResponseWithImages()
    }

    @PostMapping("/guest/checkout")
    fun guestCheckout(
        @Valid @RequestBody request: GuestCheckoutRequest,
        @RequestHeader(name = "X-Device-Id", required = false) deviceId: String?,
        @RequestHeader(name = "X-Install-Id", required = false) installId: String?,
    ): OrderResponse {
        return orderService.guestCheckout(
            command = GuestCheckoutCommand(
                items = request.items.map { GuestCheckoutItemCommand(it.productId, it.variantId, it.quantity) },
                customerName = request.customerName,
                customerPhone = request.customerPhone,
                customerEmail = request.customerEmail,
                paymentMethodCode = request.paymentMethodCode,
                deliveryMethod = request.deliveryMethod,
                deliveryAddress = request.address?.toDomain(),
                pickupPointId = request.pickupPointId,
                pickupPointExternalId = request.pickupPointExternalId,
                comment = request.comment,
                promoCode = request.promoCode,
                giftCertificateCode = request.giftCertificateCode,
            ),
            installId = deviceId?.trim()?.takeIf { it.isNotBlank() }
                ?: installId?.trim()?.takeIf { it.isNotBlank() },
        ).toResponseWithImages()
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @CurrentActorParam actor: CurrentActor,
        @PathVariable orderId: UUID,
        @RequestBody(required = false) request: CancelOrderRequest?,
    ): OrderResponse {
        return orderService.cancelOrder(
            actor = actor,
            orderId = orderId,
            comment = request?.comment,
        ).toResponseWithImages()
    }

    @GetMapping("/{orderId}")
    fun getOrder(
        @CurrentActorParam actor: CurrentActor,
        @PathVariable orderId: UUID,
    ): OrderResponse {
        return orderService.getOrder(actor, orderId).toResponseWithImages()
    }

    @GetMapping("/current")
    fun getCurrentOrders(
        @CurrentActorParam actor: CurrentActor,
    ): List<OrderResponse> {
        return orderService.getCurrentOrders(actor).toResponsesWithImages()
    }

    @GetMapping("/my")
    fun getMyOrders(
        @CurrentActorParam actor: CurrentActor,
    ): List<OrderResponse> {
        return orderService.getMyOrders(actor).toResponsesWithImages()
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
