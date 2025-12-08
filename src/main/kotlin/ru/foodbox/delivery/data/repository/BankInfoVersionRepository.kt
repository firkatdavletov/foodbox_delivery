package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.BankInfoVersionEntity

interface BankInfoVersionRepository : JpaRepository<BankInfoVersionEntity, Long> {
    fun findFirstByKey(key: String): BankInfoVersionEntity?
}