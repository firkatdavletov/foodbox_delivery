package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.data.entities.ConfirmationCodeEntity
import java.time.LocalDateTime

interface ConfirmationCodeRepository: JpaRepository<ConfirmationCodeEntity, Long> {

    fun findByPhoneAndUsedIsFalseAndConfirmedIsTrueAndExpiresAtAfter(phone: String, time: LocalDateTime): ConfirmationCodeEntity?

    fun findByCodeAndUsedIsFalseAndConfirmedIsFalseAndExpiresAtAfter(code: String, time: LocalDateTime): ConfirmationCodeEntity?

    fun findByCodeAndUsedIsFalseAndConfirmedIsTrueAndExpiresAtAfter(code: String, time: LocalDateTime): ConfirmationCodeEntity?

    @Transactional
    fun deleteAllByExpiresAtBefore(time: LocalDateTime): Long
}