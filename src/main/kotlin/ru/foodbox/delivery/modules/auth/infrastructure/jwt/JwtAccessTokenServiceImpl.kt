package ru.foodbox.delivery.modules.auth.infrastructure.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.security.UserPrincipal
import ru.foodbox.delivery.common.security.UserRole
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class JwtAccessTokenServiceImpl(
    @Value("\${jwt.secret}") jwtSecret: String,
) : JwtAccessTokenService {

    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    override fun generateAccessToken(userId: UUID, sessionId: UUID, roles: List<UserRole>, expiresAt: Instant): String {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("sid", sessionId.toString())
            .claim(ROLE_CLAIM, roles.map { it.name })
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    override fun parseAndValidate(token: String): UserPrincipal? {
        val claims = try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (_: Exception) {
            null
        } ?: return null

        val tokenType = claims[TOKEN_TYPE_CLAIM]?.toString()
        if (tokenType != ACCESS_TOKEN_TYPE) return null

        val roles = claims[ROLE_CLAIM] as? List<*> ?: return null
        val rawRoles = roles.filterIsInstance<String>()

        return UserPrincipal(
            userId = UUID.fromString(claims.subject),
            sessionId = UUID.fromString(claims["sid"].toString()),
            roles = rawRoles.toSet()
        )
    }

    companion object {
        private const val ACCESS_TOKEN_TYPE = "access"
        private const val ROLE_CLAIM = "roles"
        private const val TOKEN_TYPE_CLAIM = "tokenType"
    }
}
