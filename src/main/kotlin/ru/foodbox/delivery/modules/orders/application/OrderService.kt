package ru.foodbox.delivery.modules.orders.application

import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.orders.application.command.CheckoutCommand
import ru.foodbox.delivery.modules.orders.application.command.GuestCheckoutCommand
import ru.foodbox.delivery.modules.orders.domain.Order
import java.util.UUID

interface OrderService {
    fun checkout(actor: CurrentActor, command: CheckoutCommand): Order
    fun guestCheckout(command: GuestCheckoutCommand, installId: String?): Order
    fun cancelOrder(actor: CurrentActor, orderId: UUID, comment: String? = null): Order
    fun getOrder(actor: CurrentActor, orderId: UUID): Order
    fun getMyOrders(actor: CurrentActor): List<Order>
    fun getCurrentOrders(actor: CurrentActor): List<Order>
    fun getAdminOrders(): List<Order>
    fun getAdminOrderByNumber(orderNumber: String): Order
}
