package ru.foodbox.delivery.data.telegram.model

import org.telegram.telegrambots.meta.api.objects.Message
import ru.foodbox.delivery.data.telegram.model.StepCode

class TelegramReceivedMessageEvent(
    val chatId: Long,
    val stepCode: StepCode,
    val message: Message
)