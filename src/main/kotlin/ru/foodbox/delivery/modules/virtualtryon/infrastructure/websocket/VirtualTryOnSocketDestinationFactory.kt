package ru.foodbox.delivery.modules.virtualtryon.infrastructure.websocket

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSession

@Component
class VirtualTryOnSocketDestinationFactory {

    fun endpoint(): String = ENDPOINT

    fun destination(session: VirtualTryOnSession): String {
        return "$TOPIC_PREFIX/${session.id}/${session.subscriptionToken}"
    }

    companion object {
        const val ENDPOINT = "/ws/virtual-try-on"
        const val TOPIC_PREFIX = "/topic/virtual-try-on"
    }
}
