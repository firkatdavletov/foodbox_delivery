package ru.foodbox.delivery.data.cloudpayments_client.model

data class BankDictionaryEntity(
    val version: String,
    val dictionary: List<BankInfoEntity>
)
