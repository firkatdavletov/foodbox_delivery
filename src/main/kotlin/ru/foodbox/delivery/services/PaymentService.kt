package ru.foodbox.delivery.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.cloudpayments_client.CloudPaymentsClient
import ru.foodbox.delivery.data.cloudpayments_client.model.BankInfo
import ru.foodbox.delivery.data.cloudpayments_client.model.CryptogramPaymentRequestBody
import ru.foodbox.delivery.data.cloudpayments_client.model.SbpPaymentResponseBody
import ru.foodbox.delivery.data.cloudpayments_client.model.SbpPaymentRequestBody
import ru.foodbox.delivery.data.repository.PaymentTypeRepository
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.services.dto.BankInfoDto
import ru.foodbox.delivery.services.dto.PaymentDto
import ru.foodbox.delivery.services.dto.PaymentModelDto
import ru.foodbox.delivery.services.dto.PaymentTypeDto
import ru.foodbox.delivery.services.mapper.PaymentTypeMapper

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
                    amount = 1000.0,//amount,
                    scheme = "charge",
                    accountId = user.phone,
                    email = user.email,
                    invoiceId = orderId.toString(),
                    ipAddress = ipAddress,
                    isTest = false,
                )
                val response = paymentsClient.payBySbp(request)
                if (response.success) {
                    PaymentDto(
                        success = true,
                        message = response.message,
                        model = PaymentModelDto(
                            qrUrl = response.model?.qrUrl,
                            banks = response.model?.banks?.dictionary?.map {
                                BankInfoDto(
                                    bankName = it.bankName,
                                    logoUrl = it.logoUrl,
                                    schema = it.schema,
                                    packageName = it.packageName,
                                    webClientUrl = it.webClientUrl,
                                    isWebClientActive = it.isWebClientActive
                                )
                            }
                        )
                    )
                } else {
                    PaymentDto(
                        success = false,
                        message = response.message,
                    )
                }
            }

            "cryptogram" -> {
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
                        model = PaymentModelDto()
                    )
                } else {
                    PaymentDto(
                        success = false,
                        message = response.message
                    )
                }
            }

            else -> throw ResponseStatusException(HttpStatusCode.valueOf(403), "Unknown payment type")
        }
    }

    fun getPaymentTypesByDepartmentId(id: Long): List<PaymentTypeDto> {
        return paymentTypeRepository.getPaymentTypesByDepartmentId(id)
            .map { paymentTypeMapper.toDto(it) }
    }
}