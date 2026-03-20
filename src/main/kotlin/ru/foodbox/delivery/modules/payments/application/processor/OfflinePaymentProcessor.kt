package ru.foodbox.delivery.modules.payments.application.processor

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.payments.application.command.CreatePaymentCommand
import ru.foodbox.delivery.modules.payments.application.port.PaymentOrderContext
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus

@Component
class OfflinePaymentProcessor : PaymentProcessor {

    private val supportedMethods = listOf(
        PaymentMethodCode.CASH,
        PaymentMethodCode.CARD_ON_DELIVERY,
    )

    override fun supports(method: PaymentMethodCode): Boolean {
        return method in supportedMethods
    }

    override fun getAvailableMethods(): List<PaymentMethodInfo> {
        return supportedMethods.map { method ->
            PaymentMethodInfo(
                code = method,
                name = method.displayName,
                description = method.description,
                isOnline = method.isOnline,
                isActive = true,
            )
        }
    }

    override fun validate(
        command: CreatePaymentCommand,
        order: PaymentOrderContext,
    ) {
        if (!supports(command.paymentMethodCode)) {
            throw IllegalArgumentException("Payment method ${command.paymentMethodCode.name} is not supported")
        }
    }

    override fun preparePayment(
        command: CreatePaymentCommand,
        order: PaymentOrderContext,
    ): PreparedPayment {
        return PreparedPayment(status = PaymentStatus.AWAITING_PAYMENT)
    }
}
