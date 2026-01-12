package ru.foodbox.delivery.services

import org.springframework.stereotype.Service
import ru.foodbox.delivery.data.entities.ConfirmationCodeEntity
import ru.foodbox.delivery.data.repository.ConfirmationCodeRepository
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class ConfirmationCodeService(
    private val repository: ConfirmationCodeRepository,
) {
    fun createCodeForPhone(phone: String, duration: Long): ConfirmationCodeEntity? {
        deleteNoUsedCode(phone)

        val code = generateCode()
        val confirmationCode = saveCode(duration, phone, code, true)

        return repository.save(confirmationCode)
    }

    fun saveCheckId(phone: String, checkId: String, duration: Long): ConfirmationCodeEntity {
        deleteNoUsedCode(phone)
        val confirmationCode = saveCode(duration, phone, checkId, false)

        return repository.save(confirmationCode)
    }

    fun confirmCheckId(checkId: String): ConfirmationCodeEntity? {
        return repository.findByCodeAndUsedIsFalseAndConfirmedIsFalseAndExpiresAtAfter(checkId, LocalDateTime.now())
    }

    private fun saveCode(
        duration: Long,
        phone: String,
        code: String,
        isConfirmed: Boolean,
    ): ConfirmationCodeEntity {
        val expiresAt = LocalDateTime.now().plusMinutes(duration)

        val confirmationCode = ConfirmationCodeEntity(
            phone = phone,
            code = code,
            expiresAt = expiresAt,
            confirmed = isConfirmed,
        )
        return confirmationCode
    }

    private fun deleteNoUsedCode(phone: String) {
        val noUsedCode = repository.findByPhoneAndUsedIsFalseAndConfirmedIsTrueAndExpiresAtAfter(phone, LocalDateTime.now())

        if (noUsedCode != null) {
            repository.delete(noUsedCode)
        }
    }

    fun validateCode(phone: String, code: String): Boolean {
        val found = repository.findByPhoneAndUsedIsFalseAndConfirmedIsTrueAndExpiresAtAfter(phone,LocalDateTime.now())

        return if (found != null) {
            found.used = true
            found.confirmed = true
            repository.save(found)
            println("[VALIDATE] Код подтверждён: phone=$phone, code=$code")
            true
        } else {
            println("[VALIDATE] Ошибка подтверждения: phone=$phone, code=$code")
            false
        }
    }

    fun deleteExpiredCodes(): Long {
        val deletedCount = repository.deleteAllByExpiresAtBefore(LocalDateTime.now())
        println("[CLEANUP] Удалено просроченных кодов: $deletedCount")
        return deletedCount
    }

    private fun generateCode(): String {
        return Random.nextInt(1000, 9999).toString() // Пример: 1234
    }
}