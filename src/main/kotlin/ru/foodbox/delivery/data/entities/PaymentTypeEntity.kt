package ru.foodbox.delivery.data.entities

import jakarta.persistence.*

@Entity
@Table(name = "payment_types")
class PaymentTypeEntity(

    @Column(nullable = false)
    val key: String,

    @Column(nullable = false)
    var title: String,

    var range: Int = 0
) : BaseEntity<Long>()
