package ru.foodbox.delivery.modules.productstats.application.command

data class UpsertProductPopularityCommand(
    val enabled: Boolean,
    val manualScore: Int,
)
