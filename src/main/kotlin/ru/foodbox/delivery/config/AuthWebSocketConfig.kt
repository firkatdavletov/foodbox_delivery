package ru.foodbox.delivery.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import ru.foodbox.delivery.controllers.websockets.AuthWebSocketHandler

@Configuration
@EnableWebSocket
class AuthWebSocketConfig(
    private val handler: AuthWebSocketHandler
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(handler, "/ws/callcheck")
            .setAllowedOrigins("*")
    }
}