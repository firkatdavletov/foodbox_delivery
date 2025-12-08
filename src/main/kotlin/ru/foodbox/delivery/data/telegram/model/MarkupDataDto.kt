package ru.foodbox.delivery.data.telegram.model

/** Модель для InlineKeyboardMarkup() */
data class MarkupDataDto(
    /** Номер позиции кнопки под сообщением (Начиная с 0) **/
    val rowPos: Int = 0,
    /** Текст кнопки */
    val text: String,
    val action: Action,
) {
    enum class Action {
        CHANGE,
        CANCEL,
    }
}