package ru.foodbox.delivery.data.yandex_map_client.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class GeoObjectMetaData(
    @field:JsonProperty("GeocoderMetaData")
    val geocoderMetaData: GeocoderMetaData
)