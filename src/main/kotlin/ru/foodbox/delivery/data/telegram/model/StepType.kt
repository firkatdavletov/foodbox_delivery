package ru.foodbox.delivery.data.telegram.model

enum class StepType {
    // Простое сообщение
    SIMPLE_TEXT,
    // Сообщение с кнопкой
    INLINE_KEYBOARD_MARKUP
}