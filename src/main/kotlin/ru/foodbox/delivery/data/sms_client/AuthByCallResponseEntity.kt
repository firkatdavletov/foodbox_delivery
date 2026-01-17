package ru.foodbox.delivery.data.sms_client

import com.fasterxml.jackson.annotation.JsonProperty

class AuthByCallResponseEntity(
    val status: String,
    @field:JsonProperty("status_code")
    val statusCode: Int,
    @field:JsonProperty("check_id")
    val checkId: String,
    @field:JsonProperty("call_phone")
    val callPhone: String,
    @field:JsonProperty("call_phone_pretty")
    val callPhonePretty: String,
    @field:JsonProperty("call_phone_html")
    val callPhoneHtml: String,
)