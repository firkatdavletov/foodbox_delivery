package ru.foodbox.delivery.data.telegram

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.data.telegram.model.MarkupDataDto

@Service
class MessageService(
    private val foodBoxBot: FoodBoxBot,
    private val telegramCache: TelegramCache,
) {
    fun sendMessageToBot(
        message: String,
        orderId: Long,
        orderStatus: OrderStatus,
    ): Int? {
        val chatId = telegramCache.getChatId() ?: return null
        return foodBoxBot.sendInlineKeyboardMarkup(chatId, orderId, message, orderStatus)
    }

    // SendMessage - объект телеграм АПИ для отправки сообщения
    private fun simpleTextMessage(chatId: Long, messageText: String): SendMessage {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = messageText
        sendMessage.enableHtml(true)

        return sendMessage
    }

    fun deleteBotMessage(messageId: Int) {
        val chatId = telegramCache.getChatId() ?: return
        foodBoxBot.deleteMessage(chatId, messageId)
    }

    // Отправляем боту сообщение с кнопками
    private fun FoodBoxBot.sendInlineKeyboardMarkup(
        chatId: Long,
        orderId: Long,
        message: String,
        orderStatus: OrderStatus
    ): Int {
        val inlineKeyboardMarkup: InlineKeyboardMarkup

        val inlineKeyboardMarkupDto = when (orderStatus) {
            OrderStatus.CANCELLED -> emptyList()
            OrderStatus.DELIVERED -> emptyList()
            else -> {
                listOf(
                    MarkupDataDto(0, OrderStatus.getNextButtonText(orderStatus), MarkupDataDto.Action.CHANGE),
                    MarkupDataDto(0, "Отменить заказ", MarkupDataDto.Action.CANCEL),
                )
            }
        }

        inlineKeyboardMarkup = inlineKeyboardMarkupDto.getInlineKeyboardMarkup(orderId)

        val sentMessage = this.execute(sendMessageWithMarkup(chatId, message, inlineKeyboardMarkup))
        return sentMessage.messageId
    }

    private fun sendMessageWithMarkup(
        chatId: Long, messageText: String, inlineKeyboardMarkup: InlineKeyboardMarkup
    ): BotApiMethod<Message> {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = messageText
        sendMessage.replyMarkup = inlineKeyboardMarkup
        sendMessage.parseMode = ParseMode.HTML
        return sendMessage
    }

    // Формируем модель кнопок
    private fun List<MarkupDataDto>.getInlineKeyboardMarkup(orderId: Long): InlineKeyboardMarkup {
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val inlineKeyboardButtonsInner: MutableList<InlineKeyboardButton> = mutableListOf()
        val inlineKeyboardButtons: MutableList<MutableList<InlineKeyboardButton>> = mutableListOf()
        this.sortedBy { it.rowPos }.forEach { markupDataDto ->
            val data = "${markupDataDto.action.name}_${orderId}"
            val button = InlineKeyboardButton().apply {
                text = markupDataDto.text
                callbackData = data
            }
            inlineKeyboardButtonsInner.add(button)
        }
        inlineKeyboardButtons.add(inlineKeyboardButtonsInner)
        inlineKeyboardMarkup.keyboard = inlineKeyboardButtons
        return inlineKeyboardMarkup
    }

    fun FoodBoxBot.deleteMessage(chatId: Long, messageId: Int) {
        val deleteMessage = DeleteMessage(chatId.toString(), messageId)
        this.execute(deleteMessage)
    }
}