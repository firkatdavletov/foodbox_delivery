package ru.foodbox.delivery.modules.payments.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.ConflictException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.payments.application.command.CreatePaymentCommand
import ru.foodbox.delivery.modules.payments.application.port.OrderPaymentPort
import ru.foodbox.delivery.modules.payments.application.port.PaymentOrderSnapshot
import ru.foodbox.delivery.modules.payments.application.processor.PaymentProcessor
import ru.foodbox.delivery.modules.payments.domain.Payment
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import ru.foodbox.delivery.modules.payments.domain.repository.PaymentRepository
import java.time.Instant
import java.util.UUID

@Service
class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val orderPaymentPort: OrderPaymentPort,
    private val paymentProcessors: List<PaymentProcessor>,
) : PaymentService {

    override fun getAvailableMethods(): List<PaymentMethodInfo> {
        return paymentProcessors
            .flatMap { it.getAvailableMethods() }
            .distinctBy { it.code }
            .sortedBy { it.code.ordinal }
    }

    @Transactional
    override fun createPayment(actor: CurrentActor, command: CreatePaymentCommand): Payment {
        val order = orderPaymentPort.requireAccessibleOrder(actor, command.orderId)
        paymentRepository.findActiveByOrderId(order.id)?.let {
            throw ConflictException("Active payment already exists for order ${order.id}")
        }

        val processor = resolveProcessor(command.paymentMethodCode)
        processor.validate(command, order)
        val preparedPayment = processor.preparePayment(command, order)
        val now = Instant.now()

        val payment = Payment(
            id = UUID.randomUUID(),
            orderId = order.id,
            paymentMethodCode = command.paymentMethodCode,
            paymentMethodName = command.paymentMethodCode.displayName,
            status = preparedPayment.status,
            amountMinor = order.totalMinor,
            currency = order.currency,
            providerCode = preparedPayment.providerCode,
            externalPaymentId = preparedPayment.externalPaymentId,
            confirmationUrl = preparedPayment.confirmationUrl,
            details = normalizeDetails(command.details) ?: preparedPayment.details,
            createdAt = now,
            updatedAt = now,
            paidAt = if (preparedPayment.status == PaymentStatus.SUCCEEDED) now else null,
        )

        val savedPayment = paymentRepository.save(payment)
        orderPaymentPort.applyPaymentSnapshot(
            orderId = order.id,
            snapshot = PaymentOrderSnapshot(
                paymentMethodCode = savedPayment.paymentMethodCode,
                paymentMethodName = savedPayment.paymentMethodName,
            ),
        )
        return savedPayment
    }

    override fun getPayment(actor: CurrentActor, paymentId: UUID): Payment {
        val payment = paymentRepository.findById(paymentId)
            ?: throw NotFoundException("Payment not found")

        orderPaymentPort.requireAccessibleOrder(actor, payment.orderId)
        return payment
    }

    private fun resolveProcessor(methodCode: PaymentMethodCode): PaymentProcessor {
        return paymentProcessors.firstOrNull { it.supports(methodCode) }
            ?: throw IllegalArgumentException("Payment method ${methodCode.name} is not available")
    }

    private fun normalizeDetails(details: String?): String? {
        return details?.trim()?.takeIf { it.isNotBlank() }
    }
}
