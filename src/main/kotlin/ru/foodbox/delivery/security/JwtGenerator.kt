package ru.foodbox.delivery.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date

@Service
class JwtGenerator(
    @Value("\${jwt.secret}") jwtSecret: String,
) {

    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    val carTokenValidityMs = 365L * 24L * 60L * 60L * 1000L
    val accessTokenValidityMs = 24L * 60L * 60L * 1000L
    val refreshTokenValidityMs = 30L * 24L * 60L * 60L * 1000L

    private fun generateToken(
        id: String,
        type: String,
        expiry: Long,
    ): String {
        val now = Date()
        val expireTime = Date(now.time + expiry)

        return Jwts.builder()
            .subject(id)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expireTime)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun generateCartToken(deviceId: String): String {
        println("[JWT_GENERATOR] generateCartToken")
        return generateToken(deviceId, "cart", carTokenValidityMs)
    }

    fun generateAccessToken(userId: String): String {
        println("[JWT_GENERATOR] generateAccessToken")
        return generateToken(userId, "access", accessTokenValidityMs)
    }

    fun generateRefreshToken(userId: String): String {
        println("[JWT_GENERATOR] generateRefreshToken")
        return generateToken(userId, "refresh", refreshTokenValidityMs)
    }

    fun validateCartToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "cart"
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "access"
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "refresh"
    }

    fun getIdFromToken(token: String): String {
        val rawToken = if (token.startsWith("Bearer ")) {
            token.removePrefix("Bearer ")
        } else {
            token
        }
        val claims = parseAllClaims(rawToken) ?: throw  IllegalArgumentException("Invalid token")

        return claims.subject
    }

    private fun parseAllClaims(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (_: Exception) {
            null
        }
    }
}