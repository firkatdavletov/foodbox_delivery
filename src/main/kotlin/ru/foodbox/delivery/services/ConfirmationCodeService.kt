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
    fun createCodeForPhone(phone: String): ConfirmationCodeEntity? {
        val noUsedCode = repository.findByPhoneAndUsedIsFalseAndExpiresAtAfter(phone, LocalDateTime.now())

        if (noUsedCode != null) {
            return null
        }

        val code = "0990"//generateCode()
        val expiresAt = LocalDateTime.now().plusMinutes(1)

        val confirmationCode = ConfirmationCodeEntity(
            phone = phone,
            code = code,
            expiresAt = expiresAt
        )

        return repository.save(confirmationCode)
    }

    fun validateCode(phone: String, code: String): Boolean {
        val found = repository.findByPhoneAndUsedIsFalseAndExpiresAtAfter(phone,LocalDateTime.now())

        return if (found != null) {
            found.used = true
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