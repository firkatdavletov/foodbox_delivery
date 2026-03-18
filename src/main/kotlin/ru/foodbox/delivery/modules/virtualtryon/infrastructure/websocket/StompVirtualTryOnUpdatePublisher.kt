package ru.foodbox.delivery.modules.virtualtryon.infrastructure.websocket

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.virtualtryon.api.toResponse
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnUpdatePublisher
import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSession

@Component
class StompVirtualTryOnUpdatePublisher(
    private val messagingTemplate: SimpMessagingTemplate,
    private val socketDestinationFactory: VirtualTryOnSocketDestinationFactory,
) : VirtualTryOnUpdatePublisher {

    override fun publish(session: VirtualTryOnSession) {
        messagingTemplate.convertAndSend(
            socketDestinationFactory.destination(session),
            session.toResponse(socketDestinationFactory),
        )
    }
}
