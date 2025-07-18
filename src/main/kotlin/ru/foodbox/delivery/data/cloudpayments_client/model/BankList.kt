package ru.foodbox.delivery.data.cloudpayments_client.model

data class BankList(
    val version: String,
    val dictionary: List<BankInfo>
)
