package ru.foodbox.delivery.services.model

import com.fasterxml.jackson.annotation.JsonProperty

class CallPhoneModel(
    val status: Int,
    @field:JsonProperty("check_id")
    val checkId: String,
    @field:JsonProperty("call_phone")
    val callPhone: String,
    @field:JsonProperty("call_phone_pretty")
    val callPhonePretty: String,
    @field:JsonProperty("call_phone_html")
    val callPhoneHtml: String,
)