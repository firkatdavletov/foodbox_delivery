package ru.foodbox.delivery.data.cloudpayments_client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CryptogramResponseBody(
    @JsonProperty("Success")
    val success: Boolean,

    @JsonProperty("Message")
    val message: String? = null,

    @JsonProperty("Model")
    val model: CryptogramResponseModel? = null
)
