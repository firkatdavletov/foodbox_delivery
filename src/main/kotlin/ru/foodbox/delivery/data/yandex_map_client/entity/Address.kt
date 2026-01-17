package ru.foodbox.delivery.data.yandex_map_client.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class Address(
    @field:JsonProperty("Components")
    val components: List<AddressComponent>
)