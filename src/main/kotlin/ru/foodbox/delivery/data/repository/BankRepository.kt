package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.BankEntity

interface BankRepository : JpaRepository<BankEntity, Long> {
    fun findAllByCanStoreToken(canStoreToken: Boolean): List<BankEntity>
}