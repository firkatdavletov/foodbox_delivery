package ru.foodbox.delivery.data.cloudpayments_client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SbpPaymentResponseModel(
    @JsonProperty("Banks")
    val banks: BankDictionaryEntity,

    @JsonProperty("QrUrl")
    val qrUrl: String,

    @JsonProperty("QrImage")
    val qrImage: String? = null,

    @JsonProperty("TransactionId")
    val transactionId: Long, // id транзакции CloudPayments

    @JsonProperty("MerchantOrderId")
    val merchantOrderId: String, // переданный InvoiceId

    @JsonProperty("ProviderQrId")
    val providerQrId: String, // переданный id платежной ссылки или QR-кода

    @JsonProperty("Amount")
    val amount: Double,

    @JsonProperty("Message")
    val message: String,

    @JsonProperty("IsTest")
    val isTest: Boolean
)