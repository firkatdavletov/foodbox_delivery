package ru.foodbox.delivery.data.telegram.model

class TelegramStepMessageEvent(
    // chatId из бота
    val chatId: Long,
    // Этап или шаг в боте (стартовый, выбор кнопки, сообщение пришедшее после кнопки и тд и тп). Не путать с командами, так как в команде может быть несколько этапов
    val stepCode: StepCode
)