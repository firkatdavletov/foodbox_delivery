package ru.foodbox.delivery.modules.virtualtryon.domain.repository

import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSession
import java.util.UUID

interface VirtualTryOnSessionRepository {
    fun findById(id: UUID): VirtualTryOnSession?
    fun findByProviderPredictionId(providerPredictionId: String): VirtualTryOnSession?
    fun save(session: VirtualTryOnSession): VirtualTryOnSession
}
