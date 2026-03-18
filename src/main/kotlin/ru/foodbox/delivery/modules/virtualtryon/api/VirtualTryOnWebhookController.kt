package ru.foodbox.delivery.modules.virtualtryon.api

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.virtualtryon.api.dto.FashnWebhookRequest
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnService

@RestController
@RequestMapping("/api/v1/virtual-try-on/webhooks/fashn")
class VirtualTryOnWebhookController(
    private val virtualTryOnService: VirtualTryOnService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun handleFashnWebhook(
        @RequestParam(name = "token", required = false) token: String?,
        @RequestBody request: FashnWebhookRequest,
    ) {
        virtualTryOnService.handleWebhook(token, request.toProviderStatusResponse())
    }
}
