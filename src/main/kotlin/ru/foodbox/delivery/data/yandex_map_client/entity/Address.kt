package ru.foodbox.delivery.data.yandex_map_client.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class Address(
    @JsonProperty("Components")
    val components: List<AddressComponent>
)