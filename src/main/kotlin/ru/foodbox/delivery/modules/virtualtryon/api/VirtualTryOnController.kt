package ru.foodbox.delivery.modules.virtualtryon.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.common.web.CurrentActorParam
import ru.foodbox.delivery.modules.virtualtryon.api.dto.CreateVirtualTryOnSessionRequest
import ru.foodbox.delivery.modules.virtualtryon.api.dto.VirtualTryOnSessionResponse
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnService
import ru.foodbox.delivery.modules.virtualtryon.infrastructure.websocket.VirtualTryOnSocketDestinationFactory
import java.util.UUID

@RestController
@RequestMapping("/api/v1/virtual-try-on")
class VirtualTryOnController(
    private val virtualTryOnService: VirtualTryOnService,
    private val socketDestinationFactory: VirtualTryOnSocketDestinationFactory,
) {

    @PostMapping("/sessions")
    fun createSession(
        @CurrentActorParam actor: CurrentActor,
        @Valid @RequestBody request: CreateVirtualTryOnSessionRequest,
    ): VirtualTryOnSessionResponse {
        return virtualTryOnService.createSession(actor, request.toCommand())
            .toResponse(socketDestinationFactory)
    }

    @GetMapping("/sessions/{sessionId}")
    fun getSession(
        @CurrentActorParam actor: CurrentActor,
        @PathVariable sessionId: UUID,
    ): VirtualTryOnSessionResponse {
        return virtualTryOnService.getSession(actor, sessionId)
            .toResponse(socketDestinationFactory)
    }

    @PostMapping("/sessions/{sessionId}/sync")
    fun syncSession(
        @CurrentActorParam actor: CurrentActor,
        @PathVariable sessionId: UUID,
    ): VirtualTryOnSessionResponse {
        return virtualTryOnService.syncSession(actor, sessionId)
            .toResponse(socketDestinationFactory)
    }
}
