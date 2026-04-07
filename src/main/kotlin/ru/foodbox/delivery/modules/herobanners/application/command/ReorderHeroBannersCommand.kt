package ru.foodbox.delivery.modules.herobanners.application.command

import java.util.UUID

data class ReorderHeroBannersCommand(
    val items: List<ReorderItem>,
) {
    data class ReorderItem(
        val id: UUID,
        val sortOrder: Int,
    )
}
