package ru.foodbox.delivery.modules.orders.infrastructure.payment

import org.springframework.stereotype.Component
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderPaymentSnapshot
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.payments.application.port.OrderPaymentPort
import ru.foodbox.delivery.modules.payments.application.port.PaymentOrderContext
import ru.foodbox.delivery.modules.payments.application.port.PaymentOrderSnapshot
import java.util.UUID

@Component
class OrderPaymentPortAdapter(
    private val orderRepository: OrderRepository,
) : OrderPaymentPort {

    override fun requireAccessibleOrder(actor: CurrentActor, orderId: UUID): PaymentOrderContext {
        val order = orderRepository.findById(orderId)
            ?: throw NotFoundException("Order not found")

        if (!canAccess(actor, order)) {
            throw ForbiddenException("You do not have access to this order")
        }

        return PaymentOrderContext(
            id = order.id,
            totalMinor = order.totalMinor,
            currency = order.delivery.currency,
        )
    }

    override fun applyPaymentSnapshot(orderId: UUID, snapshot: PaymentOrderSnapshot) {
        val order = orderRepository.findById(orderId)
            ?: throw NotFoundException("Order not found")

        order.updatePaymentSnapshot(
            OrderPaymentSnapshot(
                methodCode = snapshot.paymentMethodCode,
                methodName = snapshot.paymentMethodName,
            )
        )
        orderRepository.save(order)
    }

    private fun canAccess(actor: CurrentActor, order: Order): Boolean {
        return when (actor) {
            is CurrentActor.User -> order.userId == actor.userId
            is CurrentActor.Guest -> order.guestInstallId == actor.installId
        }
    }
}
