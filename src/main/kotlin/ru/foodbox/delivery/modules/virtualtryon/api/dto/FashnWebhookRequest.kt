package ru.foodbox.delivery.modules.virtualtryon.api.dto

data class FashnWebhookRequest(
    val id: String? = null,
    val status: String? = null,
    val output: List<String>? = null,
    val error: FashnWebhookErrorRequest? = null,
)

data class FashnWebhookErrorRequest(
    val name: String? = null,
    val message: String? = null,
)
