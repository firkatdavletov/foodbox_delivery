package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.entities.RefreshTokenEntity
import ru.foodbox.delivery.data.entities.UserEntity
import ru.foodbox.delivery.data.repository.AuthTypeRepository
import ru.foodbox.delivery.data.repository.RefreshTokenRepository
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.data.sms_client.SmsClient
import ru.foodbox.delivery.security.JwtGenerator
import ru.foodbox.delivery.services.broadcast.AuthBroadcaster
import ru.foodbox.delivery.services.dto.AuthTypesDto
import ru.foodbox.delivery.services.dto.TokenPairDto
import ru.foodbox.delivery.services.model.CallPhoneModel
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

@Service
class AuthService(
    private val jwtGenerator: JwtGenerator,
    private val userRepository: UserRepository,
    private val smsClient: SmsClient,
    private val confirmationCodeService: ConfirmationCodeService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val getAuthTypeRepository: AuthTypeRepository,
    private val authBroadcaster: AuthBroadcaster,
) {
    fun sendSms(phone: String): Int? {

        val savedCode = confirmationCodeService.createCodeForPhone(phone, 1) ?: return 500

        val smsSendResponse = smsClient.sendSmsCode(savedCode.phone, savedCode.code)

        return if (smsSendResponse != null && smsSendResponse.statusCode == 100) {
            val sms = smsSendResponse.sms?.get(phone)
            sms?.statusCode ?: 500
        } else {
            500
        }
    }

    fun authByCall(phone: String): CallPhoneModel? {
        val responseEntity = smsClient.authByCall(phone) ?: return null

       val confirmationCode = confirmationCodeService.saveCheckId(phone, responseEntity.checkId, 1)

        val responseModel = CallPhoneModel(
            status = responseEntity.statusCode,
            checkId = confirmationCode.code,
            callPhone = responseEntity.callPhone,
            callPhonePretty = responseEntity.callPhonePretty,
            callPhoneHtml = responseEntity.callPhoneHtml,
        )

        return responseModel
    }

    fun callCheckStatus(checkId: String, status: Int?, createdAt: Long?) {
        val confirmationCode = confirmationCodeService.findByCode(checkId) ?: return
        val phone = confirmationCode.phone

        val tokenPair = checkUserAndCreateTokenPair(phone)

        authBroadcaster.broadcastUpdate(checkId, tokenPair)
    }

    fun verifyPhone(phone: String, code: String): TokenPairDto? {
        val isValidated = confirmationCodeService.validateCode(phone, code)

        if (isValidated) {
            return checkUserAndCreateTokenPair(phone)
        } else {
            return null
        }
    }

    private fun checkUserAndCreateTokenPair(phone: String): TokenPairDto {
        val user = userRepository.findByPhone(phone)

        val userId = if (user == null) {
            val newUser = UserEntity(phone = phone)
            newUser.created = LocalDateTime.now()
            newUser.modified = LocalDateTime.now()
            val savedUser = userRepository.save(newUser)
            savedUser.id!!
        } else {
            user.id!!
        }

        return createTokenPair(userId)
    }

    private fun createTokenPair(userId: Long): TokenPairDto {
        val newAccessToken = jwtGenerator.generateAccessToken(userId.toString())
        val newRefreshToken = jwtGenerator.generateRefreshToken(userId.toString())

        storeRefreshToken(userId, newRefreshToken)

        return TokenPairDto(newAccessToken, newRefreshToken)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPairDto? {
        if (!jwtGenerator.validateRefreshToken(refreshToken)) {
            return null
        }

        val userId = jwtGenerator.getIdFromToken(refreshToken)

        val user = userRepository.findById(userId.toLong()).getOrNull() ?: return null

        val hashed = hashToken(refreshToken)

        refreshTokenRepository.findByUserIdAndHashedToken(user.id!!, hashed)
            ?: throw ResponseStatusException(
                HttpStatusCode.valueOf(401),
                "Refresh token is not recognised (maybe used or expired?)"
            )
        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id!!, hashed)

        return createTokenPair(user.id!!)
    }

    fun logout(userId: Long): Boolean {
        val token = refreshTokenRepository.findByUserId(userId)
        if (token != null) {
            refreshTokenRepository.delete(token)
        }
        return true
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
            refreshToken.expiresAt = expiresAt
            refreshToken.hashedToken = hashedToken
            refreshTokenRepository.save(refreshToken)
        }
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}