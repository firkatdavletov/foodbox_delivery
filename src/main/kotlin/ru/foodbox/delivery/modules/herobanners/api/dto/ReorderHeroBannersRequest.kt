package ru.foodbox.delivery.modules.herobanners.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class ReorderHeroBannersRequest(
    @field:Valid
    val items: List<ReorderItemRequest>,
)

data class ReorderItemRequest(
    @field:NotNull
    val id: UUID,

    @field:NotNull
    val sortOrder: Int,
)
