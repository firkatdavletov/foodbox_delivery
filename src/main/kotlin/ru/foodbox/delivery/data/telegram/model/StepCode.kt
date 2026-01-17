package ru.foodbox.delivery.data.telegram.model

import ru.foodbox.delivery.data.telegram.model.StepType

enum class StepCode(val type: StepType, val botPause: Boolean) {
    START(StepType.SIMPLE_TEXT, false),
    USER_INFO(StepType.SIMPLE_TEXT, true),
    BUTTON_REQUEST(StepType.INLINE_KEYBOARD_MARKUP, true),
    BUTTON_RESPONSE(StepType.SIMPLE_TEXT, true)
}