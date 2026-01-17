package ru.foodbox.delivery.data.yandex_map_client.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class FeatureMember(
    @field:JsonProperty("GeoObject")
    val geoObject: GeoObject
)