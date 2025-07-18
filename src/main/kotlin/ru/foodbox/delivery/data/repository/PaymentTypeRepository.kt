package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.PaymentTypeEntity

interface PaymentTypeRepository : JpaRepository<PaymentTypeEntity, Long> {
    fun getPaymentTypesByDepartmentId(departmentId: Long): List<PaymentTypeEntity>
}