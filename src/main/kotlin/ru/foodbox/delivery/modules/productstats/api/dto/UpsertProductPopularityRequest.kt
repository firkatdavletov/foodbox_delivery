package ru.foodbox.delivery.modules.productstats.api.dto

import jakarta.validation.constraints.Min

data class UpsertProductPopularityRequest(
    val enabled: Boolean,

    @field:Min(0)
    val manualScore: Int,
)
