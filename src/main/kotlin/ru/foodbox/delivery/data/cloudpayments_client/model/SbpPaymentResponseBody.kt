package ru.foodbox.delivery.data.cloudpayments_client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SbpPaymentResponseBody(
    @JsonProperty("Model")
    val model: SbpPaymentResponseModel? = null,

    @JsonProperty("Success")
    val success: Boolean,

    @JsonProperty("Message")
    val message: String? = null
)