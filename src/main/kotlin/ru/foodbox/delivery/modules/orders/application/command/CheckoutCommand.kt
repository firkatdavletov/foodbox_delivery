package ru.foodbox.delivery.modules.orders.application.command

import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode

data class CheckoutCommand(
    val paymentMethodCode: PaymentMethodCode,
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?,
    val deliveryAddress: DeliveryAddress?,
    val comment: String?,
    val promoCode: String? = null,
    val giftCertificateCode: String? = null,
)
