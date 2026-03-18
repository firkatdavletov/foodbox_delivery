package ru.foodbox.delivery.modules.virtualtryon.application

import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSession

interface VirtualTryOnUpdatePublisher {
    fun publish(session: VirtualTryOnSession)
}
