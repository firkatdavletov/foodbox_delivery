package ru.foodbox.delivery.controllers.payment.body

import ru.foodbox.delivery.services.dto.PaymentTypeDto

data class GetPaymentTypesResponse(
    val paymentTypes: List<PaymentTypeDto>
)