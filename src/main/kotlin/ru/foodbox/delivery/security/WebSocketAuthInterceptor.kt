package ru.foodbox.delivery.security

import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class WebSocketAuthInterceptor(
    private val jwtGenerator: JwtGenerator,
) : HandshakeInterceptor {
    private val log = LoggerFactory.getLogger(WebSocketAuthInterceptor::class.java)
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String?, Any?>
    ): Boolean {
        val servletRequest = (request as ServletServerHttpRequest).servletRequest
        val authHeader = servletRequest.getHeader("Authorization")
        val token = authHeader?.removePrefix("Bearer ")?.trim()

        if (token.isNullOrBlank()) {
            log.warn("WebSocket handshake failed: no token")
            return false
        }

        if (jwtGenerator.validateAccessToken(token)) {
            val userId = jwtGenerator.getIdFromToken(authHeader)
            attributes["user_id"] = userId
            return true
        } else {
            log.warn("WebSocket handshake failed: invalidate token")
            return false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {}
}