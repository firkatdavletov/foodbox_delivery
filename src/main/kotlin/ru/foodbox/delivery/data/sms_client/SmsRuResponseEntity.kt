package ru.foodbox.delivery.data.sms_client

import com.fasterxml.jackson.annotation.JsonProperty

data class SmsRuResponseEntity(
    val status: String,
    @JsonProperty("status_code")
    val statusCode: Int,
    val sms: Map<String, SmsStatus>,
    val balance: Double
)

data class SmsStatus(
    val status: String,
    @JsonProperty("status_code")
    val statusCode: Int,
    @JsonProperty("status_text")
    val statusText: String? = null,
    @JsonProperty("sms_id")
    val smsId: String? = null
)