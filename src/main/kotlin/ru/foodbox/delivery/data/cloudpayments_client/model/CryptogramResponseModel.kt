package ru.foodbox.delivery.data.cloudpayments_client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CryptogramResponseModel(
    @JsonProperty("TransactionId")
    val transactionId: Long? = null,

    @JsonProperty("PaReq")
    val paReq: String? = null,

    @JsonProperty("GoReq")
    val goReq: String? = null,

    @JsonProperty("AcsUrl")
    val acsUrl: String? = null,

    @JsonProperty("ThreeDsSessionData")
    val threeDsSessionData: String? = null,

    @JsonProperty("IFrameIsAllowed")
    val iFrameIsAllowed: Boolean? = null,

    @JsonProperty("FrameWidth")
    val frameWidth: Int? = null,

    @JsonProperty("FrameHeight")
    val frameHeight: Int? = null,

    @JsonProperty("ThreeDsCallbackId")
    val threeDsCallbackId: String? = null,

    @JsonProperty("EscrowAccumulationId")
    val escrowAccumulationId: String? = null,

    // --- Поля транзакции ---
    @JsonProperty("ReasonCode")
    val reasonCode: Int? = null,

    @JsonProperty("PublicId")
    val publicId: String? = null,

    @JsonProperty("TerminalUrl")
    val terminalUrl: String? = null,

    @JsonProperty("Amount")
    val amount: Double? = null,

    @JsonProperty("Currency")
    val currency: String? = null,

    @JsonProperty("CurrencyCode")
    val currencyCode: Int? = null,

    @JsonProperty("PaymentAmount")
    val paymentAmount: Double? = null,

    @JsonProperty("PaymentCurrency")
    val paymentCurrency: String? = null,

    @JsonProperty("PaymentCurrencyCode")
    val paymentCurrencyCode: Int? = null,

    @JsonProperty("InvoiceId")
    val invoiceId: String? = null,

    @JsonProperty("AccountId")
    val accountId: String? = null,

    @JsonProperty("Email")
    val email: String? = null,

    @JsonProperty("Description")
    val description: String? = null,

    @JsonProperty("JsonData")
    val jsonData: Map<String, String>? = null,

    @JsonProperty("CreatedDate")
    val createdDate: String? = null,

    @JsonProperty("PayoutDate")
    val payoutDate: String? = null,

    @JsonProperty("PayoutDateIso")
    val payoutDateIso: String? = null,

    @JsonProperty("PayoutAmount")
    val payoutAmount: Double? = null,

    @JsonProperty("CreatedDateIso")
    val createdDateIso: String? = null,

    @JsonProperty("AuthDate")
    val authDate: String? = null,

    @JsonProperty("AuthDateIso")
    val authDateIso: String? = null,

    @JsonProperty("ConfirmDate")
    val confirmDate: String? = null,

    @JsonProperty("ConfirmDateIso")
    val confirmDateIso: String? = null,

    @JsonProperty("AuthCode")
    val authCode: String? = null,

    @JsonProperty("TestMode")
    val testMode: Boolean? = null,

    @JsonProperty("Rrn")
    val rrn: String? = null,

    @JsonProperty("OriginalTransactionId")
    val originalTransactionId: Long? = null,

    @JsonProperty("FallBackScenarioDeclinedTransactionId")
    val fallBackScenarioDeclinedTransactionId: Long? = null,

    @JsonProperty("IpAddress")
    val ipAddress: String? = null,

    @JsonProperty("IpCountry")
    val ipCountry: String? = null,

    @JsonProperty("IpCity")
    val ipCity: String? = null,

    @JsonProperty("IpRegion")
    val ipRegion: String? = null,

    @JsonProperty("IpDistrict")
    val ipDistrict: String? = null,

    @JsonProperty("IpLatitude")
    val ipLatitude: Double? = null,

    @JsonProperty("IpLongitude")
    val ipLongitude: Double? = null,

    @JsonProperty("CardFirstSix")
    val cardFirstSix: String? = null,

    @JsonProperty("CardLastFour")
    val cardLastFour: String? = null,

    @JsonProperty("CardExpDate")
    val cardExpDate: String? = null,

    @JsonProperty("CardType")
    val cardType: String? = null,

    @JsonProperty("CardProduct")
    val cardProduct: String? = null,

    @JsonProperty("CardCategory")
    val cardCategory: String? = null,

    @JsonProperty("IssuerBankCountry")
    val issuerBankCountry: String? = null,

    @JsonProperty("Issuer")
    val issuer: String? = null,

    @JsonProperty("CardTypeCode")
    val cardTypeCode: Int? = null,

    @JsonProperty("Status")
    val status: String? = null,

    @JsonProperty("StatusCode")
    val statusCode: Int? = null,

    @JsonProperty("CultureName")
    val cultureName: String? = null,

    @JsonProperty("Reason")
    val reason: String? = null,

    @JsonProperty("CardHolderMessage")
    val cardHolderMessage: String? = null,

    @JsonProperty("Type")
    val type: Int? = null,

    @JsonProperty("Refunded")
    val refunded: Boolean? = null,

    @JsonProperty("Name")
    val name: String? = null,

    @JsonProperty("Token")
    val token: String? = null,

    @JsonProperty("SubscriptionId")
    val subscriptionId: String? = null,

    @JsonProperty("GatewayName")
    val gatewayName: String? = null,

    @JsonProperty("AndroidPay")
    val androidPay: Boolean? = null,

    @JsonProperty("WalletType")
    val walletType: String? = null,

    @JsonProperty("TotalFee")
    val totalFee: Int? = null
)
