package ru.foodbox.delivery.modules.productstats.api.dto

import java.util.UUID

data class ReorderProductPopularityRequest(
    val productIds: List<UUID>,
)
