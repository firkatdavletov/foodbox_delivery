package ru.foodbox.delivery.data.telegram

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import ru.foodbox.delivery.data.telegram.model.ButtonClickEvent
import ru.foodbox.delivery.data.telegram.model.MarkupDataDto
import ru.foodbox.delivery.data.telegram.model.RawUpdateEvent

@Service
class ReceiverService(
    private val publisher: ApplicationEventPublisher,
) {
    @EventListener
    fun onUpdate(event: RawUpdateEvent) {
        execute(event.update)
    }
    // выходной метод сервиса
    fun execute(update: Update) {
        if (update.hasCallbackQuery()) { // Выполнить, если это действие по кнопке
            callbackExecute(update.callbackQuery)
        } else if (update.hasMessage()) { // Выполнить, если это сообщение пользователя
            messageExecute(update.message)
        } else {
            throw IllegalStateException("Not yet supported")
        }
    }

    private fun messageExecute(message: Message) {

    }

    private fun callbackExecute(callback: CallbackQuery) {
        val data = callback.data.split("_")
        val action = data.firstOrNull() ?: return

        when (action) {
            MarkupDataDto.Action.CHANGE.name -> {
                val orderId = data.component2().toLongOrNull() ?: return
                publisher.publishEvent(ButtonClickEvent(orderId, MarkupDataDto.Action.CHANGE))
            }
            MarkupDataDto.Action.CANCEL.name -> {
                val orderId = data.component2().toLongOrNull() ?: return
                publisher.publishEvent(ButtonClickEvent(orderId, MarkupDataDto.Action.CANCEL))
            }
        }
    }
}