package ru.foodbox.delivery.data.cloudpayments_client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PayerModel(
    @JsonProperty("FirstName")
    val firstName: String? = null,

    @JsonProperty("LastName")
    val lastName: String? = null,

    @JsonProperty("MiddleName")
    val middleName: String? = null,

    @JsonProperty("Birth")
    val birth: String? = null, // Формат даты зависит от API

    @JsonProperty("Street")
    val street: String? = null,

    @JsonProperty("Address")
    val address: String? = null,

    @JsonProperty("City")
    val city: String? = null,

    @JsonProperty("Country")
    val country: String? = null,

    @JsonProperty("Phone")
    val phone: String? = null,

    @JsonProperty("Postcode")
    val postcode: String? = null
)