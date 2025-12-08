package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "banks")
class BankEntity(

    @Column(name = "name", nullable = false)
    val bankName: String,

    @Column(name = "logo_url", nullable = false)
    val logoUrl: String,

    @Column(name = "schema", nullable = false)
    val schema: String,

    @Column(name = "package_name", nullable = true)
    val packageName: String? = null,

    @Column(name = "can_store_token")
    val canStoreToken: Boolean,
) : BaseEntity<Long>()