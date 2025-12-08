package ru.foodbox.delivery.data.telegram

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.foodbox.delivery.data.telegram.model.RawUpdateEvent

@Component
class FoodBoxBot(
    @Value("\${telegram.token}")
    private val token: String,
    @Value("telegram.bot_name")
    private val botName: String,
    private val commands: Set<BotCommand>,
    private val publisher: ApplicationEventPublisher,
) : TelegramLongPollingCommandBot(
    token,
) {

    @PostConstruct
    fun initCommand() {
        commands.forEach { register(it) }

        registerDefaultAction { absSender, message ->
            val commandUnknownMessage = SendMessage()
            commandUnknownMessage.chatId = message.chatId.toString()
            commandUnknownMessage.text = "Command '" + message.text.toString() + "' unknown"

            absSender.execute(commandUnknownMessage)
        }
    }

    override fun getBotUsername(): String? = botName

    override fun processNonCommandUpdate(update: Update?) {
        if (update != null) {
            publisher.publishEvent(RawUpdateEvent(update))
        }
    }
}