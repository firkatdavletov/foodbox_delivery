package ru.foodbox.delivery.modules.virtualtryon.infrastructure.fashn

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnWebhookTokenVerifier

@Component
class FashnWebhookTokenVerifier(
    private val properties: FashnVirtualTryOnProperties,
) : VirtualTryOnWebhookTokenVerifier {

    override fun isValid(token: String?): Boolean {
        val configured = properties.webhookSecret.trim()
        if (configured.isBlank()) {
            return false
        }
        return configured == token?.trim()
    }
}
