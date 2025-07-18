package ru.foodbox.delivery.data.cloudpayments_client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class BankInfo(
    @JsonProperty("bankName")
    val bankName: String,

    @JsonProperty("logoURL")
    val logoUrl: String,

    @JsonProperty("schema")
    val schema: String,

    @JsonProperty("package_name")
    val packageName: String?,

    @JsonProperty("webClientUrl")
    val webClientUrl: String?,

    @JsonProperty("isWebClientActive")
    val isWebClientActive: String? // Или Boolean, если на сервере реально `true/false`, а не строка
)