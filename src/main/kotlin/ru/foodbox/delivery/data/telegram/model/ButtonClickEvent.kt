package ru.foodbox.delivery.data.telegram.model

data class ButtonClickEvent(
    val orderId: Long,
    val action: MarkupDataDto.Action
)