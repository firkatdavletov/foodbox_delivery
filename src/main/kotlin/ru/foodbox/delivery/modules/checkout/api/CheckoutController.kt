package ru.foodbox.delivery.modules.checkout.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.checkout.api.dto.CheckoutOptionsResponse
import ru.foodbox.delivery.modules.checkout.application.CheckoutService

@RestController
@RequestMapping("/api/v1/checkout")
class CheckoutController(
    private val checkoutService: CheckoutService,
) {

    @GetMapping("/options")
    fun getOptions(): CheckoutOptionsResponse {
        return checkoutService.getAvailableOptions().toResponse()
    }
}
