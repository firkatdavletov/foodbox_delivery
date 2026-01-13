package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.ConfirmationCodeEntity
import java.time.LocalDateTime

interface ConfirmationCodeRepository: JpaRepository<ConfirmationCodeEntity, Long> {

    fun findByPhoneAndUsedIsFalseAndConfirmedIsTrueAndExpiresAtAfter(phone: String, time: LocalDateTime): ConfirmationCodeEntity?

    fun findByCodeAndUsedIsFalseAndConfirmedIsFalseAndExpiresAtAfter(code: String, time: LocalDateTime): ConfirmationCodeEntity?

    fun findByCodeAndUsedIsFalseAndConfirmedIsTrueAndExpiresAtAfter(code: String, time: LocalDateTime): ConfirmationCodeEntity?

    fun deleteAllByExpiresAtBefore(time: LocalDateTime): Long
}