package ru.foodbox.delivery.modules.checkout.api

import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.checkout.api.dto.CheckoutOptionsResponse
import ru.foodbox.delivery.modules.checkout.application.CheckoutOptionsQuery
import ru.foodbox.delivery.modules.checkout.application.CheckoutService

@Validated
@RestController
@RequestMapping("/api/v1/checkout")
class CheckoutController(
    private val checkoutService: CheckoutService,
) {

    @GetMapping("/options")
    fun getOptions(
        @RequestParam(name = "yandexGeoId", required = false)
        @Min(0)
        yandexGeoId: Long?,
    ): CheckoutOptionsResponse {
        return checkoutService.getAvailableOptions(
            CheckoutOptionsQuery(
                yandexGeoId = yandexGeoId,
            )
        ).toResponse()
    }
}
