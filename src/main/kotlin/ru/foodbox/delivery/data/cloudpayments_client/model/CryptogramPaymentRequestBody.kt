package ru.foodbox.delivery.data.cloudpayments_client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CryptogramPaymentRequestBody(
    @JsonProperty("Amount")
    val amount: Double, // Обязательный, формат XX.XX

    @JsonProperty("IpAddress")
    val ipAddress: String, // Обязательный

    @JsonProperty("CardCryptogramPacket")
    val cardCryptogramPacket: String, // Обязательный

    @JsonProperty("Currency")
    val currency: String? = null, // RUB/USD/EUR/GBP, по умолчанию RUB

    @JsonProperty("Name")
    val name: String? = null, // Имя держателя карты (латиницей)

    @JsonProperty("PaymentUrl")
    val paymentUrl: String? = null, // Адрес сайта вызова

    @JsonProperty("InvoiceId")
    val invoiceId: String? = null, // Номер счета

    @JsonProperty("Description")
    val description: String? = null, // Описание оплаты

    @JsonProperty("CultureName")
    val cultureName: String? = null, // ru-RU / en-US

    @JsonProperty("AccountId")
    val accountId: String? = null, // Идентификатор пользователя

    @JsonProperty("Email")
    val email: String? = null, // Email плательщика

    @JsonProperty("Payer")
    val payerModel: PayerModel? = null, // Доп. информация о плательщике

    @JsonProperty("JsonData")
    val jsonData: Map<String, String>? = null, // Доп. данные транзакции

    @JsonProperty("SaveCard")
    val saveCard: Boolean? = null // true / false (по умолчанию false)
)
