package ru.foodbox.delivery.modules.auth.infrastructure.jwt

import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Service
class RefreshTokenServiceImpl: RefreshTokenService {

    private val secureRandom = SecureRandom()

    override fun generateRawToken(): String {
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    override fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawToken.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}