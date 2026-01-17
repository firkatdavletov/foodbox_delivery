package ru.foodbox.delivery.data.telegram.model

import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.foodbox.delivery.data.telegram.model.StepCode

class TelegramReceivedCallbackEvent(
    val chatId: Long,
    val stepCode: StepCode,
    val callback: CallbackQuery
)