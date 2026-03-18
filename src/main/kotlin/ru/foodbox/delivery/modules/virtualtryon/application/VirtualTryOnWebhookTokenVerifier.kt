package ru.foodbox.delivery.modules.virtualtryon.application

interface VirtualTryOnWebhookTokenVerifier {
    fun isValid(token: String?): Boolean
}
