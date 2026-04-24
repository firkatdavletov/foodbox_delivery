package ru.foodbox.delivery.modules.productstats.application.command

import java.util.UUID

data class ReorderProductPopularityCommand(
    val productIds: List<UUID>,
)
