package ru.foodbox.delivery.common.security

import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import ru.foodbox.delivery.modules.auth.infrastructure.jwt.JwtAccessTokenServiceImpl

@Component
class WebSocketAuthInterceptor(
    private val jwtTokenService: JwtAccessTokenServiceImpl,
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

        val principal = jwtTokenService.parseAndValidate(token)

        if (principal != null) {
            attributes["user_id"] = principal.userId
            return true
        } else {
            log.warn("WebSocket handshake failed: no user")
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