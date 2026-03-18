package ru.foodbox.delivery.modules.virtualtryon.application

import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.virtualtryon.application.command.CreateVirtualTryOnSessionCommand
import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSession
import java.util.UUID

interface VirtualTryOnService {
    fun createSession(actor: CurrentActor, command: CreateVirtualTryOnSessionCommand): VirtualTryOnSession
    fun getSession(actor: CurrentActor, sessionId: UUID): VirtualTryOnSession
    fun syncSession(actor: CurrentActor, sessionId: UUID): VirtualTryOnSession
    fun handleWebhook(token: String?, payload: VirtualTryOnProviderStatusResponse)
}
