package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.foodbox.delivery.data.entities.PaymentTypeEntity

interface PaymentTypeRepository : JpaRepository<PaymentTypeEntity, Long> {
    @Query(
        value = "SELECT * FROM payment_types",
        nativeQuery = true
    )
    fun getPaymentTypes(): List<PaymentTypeEntity>
}