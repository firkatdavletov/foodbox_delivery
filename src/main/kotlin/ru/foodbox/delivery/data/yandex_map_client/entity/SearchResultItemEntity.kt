package ru.foodbox.delivery.data.yandex_map_client.entity

import org.springframework.data.geo.Distance

data class SearchResultItemEntity(
    val address: SearchResultAddressEntity,
    val distance: DistanceEntity,
    val uri: String,
)