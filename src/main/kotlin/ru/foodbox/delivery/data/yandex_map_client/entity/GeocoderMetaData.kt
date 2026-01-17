package ru.foodbox.delivery.data.yandex_map_client.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class GeocoderMetaData(
    @field:JsonProperty("Address")
    val address: Address,
)