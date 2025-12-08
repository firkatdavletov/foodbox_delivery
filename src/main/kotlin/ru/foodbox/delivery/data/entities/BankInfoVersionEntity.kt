package ru.foodbox.delivery.data.entities

import jakarta.persistence.Entity

@Entity
class BankInfoVersionEntity(
    val key: String,
    var version: String
) : BaseEntity<Long>()