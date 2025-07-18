package ru.foodbox.delivery.data.cloudpayments_client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SbpPaymentRequestBody(
    @JsonProperty("PublicId")
    val publicId: String,

    @JsonProperty("Amount")
    val amount: Double,

    @JsonProperty("Currency")
    val currency: String = "RUB",

    @JsonProperty("Description")
    val description: String? = null,

    @JsonProperty("AccountId")
    val accountId: String? = null,

    @JsonProperty("Email")
    val email: String? = null,

    @JsonProperty("JsonData")
    val jsonData: PayerModel? = null, // можно использовать JsonObject или любую структуру

    @JsonProperty("InvoiceId")
    val invoiceId: String? = null,

    @JsonProperty("Scheme")
    val scheme: String, // "charge"

    @JsonProperty("SuccessRedirectUrl")
    val successRedirectUrl: String? = null,

    @JsonProperty("IpAddress")
    val ipAddress: String? = null,

    @JsonProperty("Os")
    val os: String? = null,

    @JsonProperty("Webview")
    val webview: Boolean? = null,

    @JsonProperty("Device")
    val device: String? = null, // MobileApp, DesktopWeb, Mobile

    @JsonProperty("Browser")
    val browser: String? = null,

    @JsonProperty("TtlMinutes")
    val ttlMinutes: Int? = null,

    @JsonProperty("SaveCard")
    val saveCard: Boolean? = null,

    @JsonProperty("IsTest")
    val isTest: Boolean? = null
)
