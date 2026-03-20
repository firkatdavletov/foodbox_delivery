package ru.foodbox.delivery.modules.payments.application

import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.payments.application.command.CreatePaymentCommand
import ru.foodbox.delivery.modules.payments.domain.Payment
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo
import java.util.UUID

interface PaymentService {
    fun getAvailableMethods(): List<PaymentMethodInfo>
    fun createPayment(actor: CurrentActor, command: CreatePaymentCommand): Payment
    fun getPayment(actor: CurrentActor, paymentId: UUID): Payment
}
