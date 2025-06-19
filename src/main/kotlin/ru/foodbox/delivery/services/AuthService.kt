package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.entities.CartEntity
import ru.foodbox.delivery.data.entities.UserEntity
import ru.foodbox.delivery.data.repository.AuthTypeRepository
import ru.foodbox.delivery.data.repository.CartRepository
import ru.foodbox.delivery.data.repository.DepartmentRepository
import ru.foodbox.delivery.data.repository.RefreshTokenRepository
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.services.dto.AuthTypesDto
import ru.foodbox.delivery.data.entities.RefreshTokenEntity
import ru.foodbox.delivery.data.sms_client.SmsClient
import ru.foodbox.delivery.data.sms_client.SmsRuResponseEntity
import ru.foodbox.delivery.services.dto.TokenPairDto
import ru.foodbox.delivery.security.JwtGenerator
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class AuthService(
    private val jwtGenerator: JwtGenerator,
    private val userRepository: UserRepository,
    private val smsClient: SmsClient,
    private val cartRepository: CartRepository,
    private val confirmationCodeService: ConfirmationCodeService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val getAuthTypeRepository: AuthTypeRepository,
    private val departmentRepository: DepartmentRepository,
) {
    fun sendSms(phone: String): Int {

        val saved = confirmationCodeService.createCodeForPhone(phone)

        val smsSendResponse = if (phone == "79000000000") {
            SmsRuResponseEntity(status = "true", statusCode = 100, sms = mapOf(), balance = 0.0)
        } else {
            smsClient.sendSmsCode(saved.phone, saved.code)
        }

        return smsSendResponse.statusCode
    }

    fun verifyPhone(phone: String, code: String): TokenPairDto {
        val isValidated = confirmationCodeService.validateCode(phone, code)

        if (isValidated) {
            val user = userRepository.findByPhone(phone)

            return if (user == null) {
                val defaultDepartment = departmentRepository.findById(1).orElseThrow {
                    ResponseStatusException(HttpStatusCode.valueOf(500), "Не найден магазин")
                }
                val newUser = userRepository.save(UserEntity(
                    phone = phone,
                ))
                val newCart = CartEntity(user = newUser, department = defaultDepartment)
                cartRepository.save(newCart)
                createTokenPair(newUser.id)
            } else {
                createTokenPair(user.id)
            }
        } else {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid confirmation code")
        }
    }

    private fun createTokenPair(userId: Long): TokenPairDto {
        val newAccessToken = jwtGenerator.generateAccessToken(userId.toString())
        val newRefreshToken = jwtGenerator.generateRefreshToken(userId.toString())

        storeRefreshToken(userId, newRefreshToken)

        return TokenPairDto(newAccessToken, newRefreshToken)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPairDto {
        if (!jwtGenerator.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token")
        }

        val userId = jwtGenerator.getUserIdFromToken(refreshToken)

        val user = userRepository.findById(userId.toLong()).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(404), "Invalid refresh token")
        }

        val hashed = hashToken(refreshToken)

        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed)
            ?: throw ResponseStatusException(
                HttpStatusCode.valueOf(401),
                "Refresh token is not recognised (maybe used or expired?)"
            )
        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        return createTokenPair(user?.id!!)
    }

    fun getAuthTypes(): AuthTypesDto {
        return AuthTypesDto(
            types = getAuthTypeRepository.findAll().map { it.name }
        )
    }

    private fun storeRefreshToken(userId: Long, rawRefreshToken: String) {
        val hashedToken = hashToken(rawRefreshToken)
        val expiryMs = jwtGenerator.refreshTokenValidityMs
        val expiresAt = LocalDateTime
            .now()
            .plusNanos(TimeUnit.NANOSECONDS.convert(expiryMs, TimeUnit.MILLISECONDS))

        val refreshToken = refreshTokenRepository.findByUserId(userId)

        if (refreshToken == null) {
            refreshTokenRepository.save(
                RefreshTokenEntity(
                    userId = userId,
                    expiresAt = expiresAt,
                    hashedToken = hashedToken
                )
            )
        } else {
            val updatedToken = refreshToken.copy(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashedToken
            )
            refreshTokenRepository.save(updatedToken)
        }
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}