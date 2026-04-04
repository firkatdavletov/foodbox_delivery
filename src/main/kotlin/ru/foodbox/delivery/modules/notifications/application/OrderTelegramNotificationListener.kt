package ru.foodbox.delivery.modules.notifications.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import ru.foodbox.delivery.modules.orders.application.event.OrderCreatedEvent
import ru.foodbox.delivery.modules.orders.application.event.OrderStatusChangedEvent

@Component
class OrderTelegramNotificationListener(
    private val formatter: TelegramOrderMessageFormatter,
    private val telegramNotificationService: TelegramNotificationService,
) {

    private val logger = LoggerFactory.getLogger(OrderTelegramNotificationListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderCreated(event: OrderCreatedEvent) {
        if (!event.order.currentStatus.notifyStaff) {
            return
        }
        try {
            val message = formatter.formatOrderCreated(event.order)
            telegramNotificationService.sendToDefaultChats(message)
        } catch (ex: Exception) {
            logger.warn(
                "Failed to send Telegram notification for created order {} due to {}",
                event.order.orderNumber,
                ex.javaClass.simpleName,
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderStatusChanged(event: OrderStatusChangedEvent) {
        if (!event.currentStatus.notifyStaff) {
            return
        }
        try {
            val message = formatter.formatOrderStatusChanged(
                order = event.order,
                previousStatus = event.previousStatus,
            )
            telegramNotificationService.sendToDefaultChats(message)
        } catch (ex: Exception) {
            logger.warn(
                "Failed to send Telegram notification for updated order {} due to {}",
                event.order.orderNumber,
                ex.javaClass.simpleName,
            )
        }
    }
}
