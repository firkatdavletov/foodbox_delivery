package ru.foodbox.delivery.data.telegram.command

import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import ru.foodbox.delivery.data.telegram.TelegramCache
import ru.foodbox.delivery.data.telegram.command.CommandName

@Component
class StartCommand(
    private val telegramCache: TelegramCache,
) : BotCommand(CommandName.START.text, CommandName.START.desc) {
    fun createMessage(chatId: String, text: String) =
        SendMessage(chatId, text)
            .apply { enableMarkdown(true) }
            .apply { disableWebPagePreview() }

    override fun execute(
        absSender: AbsSender?,
        user: User?,
        chat: Chat?,
        params: Array<out String?>?
    ) {
        if (chat?.id != null) {
            telegramCache.saveChatId(chat.id)
            absSender?.execute(createMessage(chat.id.toString(), "Бот подключен"))
        }
    }
}