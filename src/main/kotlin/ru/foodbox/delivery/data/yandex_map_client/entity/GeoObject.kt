package ru.foodbox.delivery.data.yandex_map_client.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class GeoObject(
    val metaDataProperty: GeoObjectMetaData,
    val name: String,
    val description: String,
    val uri: String,
    @field:JsonProperty("Point")
    val point: GeoPoint
)