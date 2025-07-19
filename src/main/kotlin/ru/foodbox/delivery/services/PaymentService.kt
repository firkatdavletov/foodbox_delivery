package ru.foodbox.delivery.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.cloudpayments_client.CloudPaymentsClient
import ru.foodbox.delivery.data.cloudpayments_client.model.BankInfo
import ru.foodbox.delivery.data.cloudpayments_client.model.CryptogramPaymentRequestBody
import ru.foodbox.delivery.data.cloudpayments_client.model.SbpPaymentRequestBody
import ru.foodbox.delivery.data.repository.PaymentTypeRepository
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.services.dto.BankInfoDto
import ru.foodbox.delivery.services.dto.PaymentDto
import ru.foodbox.delivery.services.dto.PaymentModelDto
import ru.foodbox.delivery.services.dto.PaymentTypeDto
import ru.foodbox.delivery.services.mapper.PaymentTypeMapper
import java.util.UUID

@Service
class PaymentService(
    @Value("\${cloud.payments.public.id}") private val publicId: String,
    private val paymentsClient: CloudPaymentsClient,
    private val paymentTypeRepository: PaymentTypeRepository,
    private val userRepository: UserRepository,
    private val paymentTypeMapper: PaymentTypeMapper,
) {
    fun pay(
        paymentType: String,
        token: String?,
        cryptogram: String?,
        ipAddress: String,
        amount: Double,
        userId: Long,
        orderId: Long,
    ): PaymentDto {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatusCode.valueOf(404), "unknown user") }

        return when (paymentType) {
            "sbp" -> {
                val request = SbpPaymentRequestBody(
                    publicId = publicId,
                    amount = amount,
                    scheme = "charge",
//                    accountId = user.phone,
                    email = user.email,
                    invoiceId = orderId.toString(),
                    ipAddress = ipAddress,
                    saveCard = false,
                    isTest = false,
                )
                val response = paymentsClient.payBySbp(request)
                if (response.success) {
                    PaymentDto(
                        success = true,
                        message = response.message,
                        paymentType = paymentType,
                        model = PaymentModelDto(
                            qrUrl = response.model?.qrUrl,
                            orderId = response.model?.merchantOrderId?.toLongOrNull(),
                        )
                    )
                } else {
                    PaymentDto(
                        success = false,
                        paymentType = paymentType,
                        message = response.message,
                    )
                }
            }

            "card" -> {
                val request = CryptogramPaymentRequestBody(
                    amount = amount,
                    ipAddress = ipAddress,
                    cardCryptogramPacket = cryptogram ?: throw ResponseStatusException(
                        HttpStatusCode.valueOf(403),
                        "Cryptogram is null"
                    ),
                    name = user.name,
                    invoiceId = orderId.toString(),
                    accountId = user.phone,
                    email = user.email,
                )
                val response = paymentsClient.payByCryptogram(request)
                if (response.success) {
                    PaymentDto(
                        success = true,
                        message = response.message,
                        paymentType = paymentType,
                        model = PaymentModelDto(
                            orderId = response.model?.invoiceId?.toLongOrNull(),
                        )
                    )
                } else {
                    PaymentDto(
                        success = false,
                        message = response.message,
                        paymentType = paymentType,
                    )
                }
            }

            "cash" -> {
                return PaymentDto(
                    success = true,
                    model = PaymentModelDto(
                        orderId = orderId,
                    ),
                    message = null,
                    paymentType = paymentType
                )
            }

            else -> throw ResponseStatusException(HttpStatusCode.valueOf(403), "Unknown payment type")
        }
    }

    fun getPaymentTypesByDepartmentId(): List<PaymentTypeDto> {
        return paymentTypeRepository.getPaymentTypes()
            .map { paymentTypeMapper.toDto(it) }
    }
}