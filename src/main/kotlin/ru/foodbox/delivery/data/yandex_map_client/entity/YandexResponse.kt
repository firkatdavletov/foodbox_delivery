package ru.foodbox.delivery.data.yandex_map_client.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class YandexResponse(
    @JsonProperty("GeoObjectCollection")
    val collection: GeoObjectCollection
)