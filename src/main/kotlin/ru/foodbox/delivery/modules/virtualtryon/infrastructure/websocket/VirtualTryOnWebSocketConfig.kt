package ru.foodbox.delivery.modules.virtualtryon.infrastructure.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import ru.foodbox.delivery.common.security.CorsProps

@Configuration
@EnableWebSocketMessageBroker
class VirtualTryOnWebSocketConfig(
    private val corsProps: CorsProps,
) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint(VirtualTryOnSocketDestinationFactory.ENDPOINT)
            .setAllowedOriginPatterns(*resolveAllowedOrigins().toTypedArray())
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker(VirtualTryOnSocketDestinationFactory.TOPIC_PREFIX)
        registry.setApplicationDestinationPrefixes("/app")
    }

    private fun resolveAllowedOrigins(): List<String> {
        val origins = (corsProps.siteAllowedOrigins + corsProps.adminAllowedOrigins)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

        return origins.ifEmpty { listOf("*") }
    }
}
