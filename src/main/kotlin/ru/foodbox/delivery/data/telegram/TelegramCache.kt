package ru.foodbox.delivery.data.telegram

import org.springframework.stereotype.Component

@Component
class TelegramCache {
    private var _chatId: Long? = null

    fun saveChatId(chatId: Long) {
        _chatId = chatId
    }

    fun getChatId(): Long? = _chatId
}