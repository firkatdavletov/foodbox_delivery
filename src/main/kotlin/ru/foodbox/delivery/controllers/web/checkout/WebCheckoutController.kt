package ru.foodbox.delivery.controllers.web.checkout

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.web.checkout.body.GuestCheckoutRequestBody
import ru.foodbox.delivery.controllers.web.checkout.body.GuestCheckoutResponseBody
import ru.foodbox.delivery.services.OrderService
import ru.foodbox.delivery.services.dto.GuestCheckoutCustomerInputDto
import ru.foodbox.delivery.services.dto.GuestCheckoutDeliveryInputDto
import ru.foodbox.delivery.services.dto.GuestCheckoutItemInputDto
import org.springframework.http.HttpStatusCode

@RestController
@RequestMapping("/api/web/checkout")
class WebCheckoutController(
    private val orderService: OrderService,
) {

    @PostMapping("/guest")
    fun guestCheckout(@Valid @RequestBody request: GuestCheckoutRequestBody): ResponseEntity<GuestCheckoutResponseBody> {
        // TODO: добавить rate limiting / anti-spam для публичного guest-checkout endpoint.
        val customer = request.customer
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(400), "Данные покупателя обязательны")
        val delivery = request.delivery
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(400), "Данные доставки обязательны")
        val deliveryType = delivery.type
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(400), "Тип доставки обязателен")

        val result = orderService.createGuestOrder(
            items = request.items.map { item ->
                GuestCheckoutItemInputDto(
                    productId = item.productId,
                    sku = item.sku,
                    quantity = item.quantity
                )
            },
            customer = GuestCheckoutCustomerInputDto(
                name = customer.name ?: "",
                phone = customer.phone ?: "",
                email = customer.email,
            ),
            delivery = GuestCheckoutDeliveryInputDto(
                type = deliveryType,
                address = delivery.address,
                pickupPointId = delivery.pickupPointId,
            ),
            comment = request.comment,
        )

        return ResponseEntity.ok(GuestCheckoutResponseBody(result))
    }
}
