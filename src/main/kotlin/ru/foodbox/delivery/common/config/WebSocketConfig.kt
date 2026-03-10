package ru.foodbox.delivery.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import ru.foodbox.delivery.controllers.websockets.OrderStatusWebSocketHandler
import ru.foodbox.delivery.common.security.WebSocketAuthInterceptor

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val handler: OrderStatusWebSocketHandler,
    private val interceptor: WebSocketAuthInterceptor,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(handler, "/ws/orders")
            .addInterceptors(interceptor)
            .setAllowedOrigins("*")
    }
}