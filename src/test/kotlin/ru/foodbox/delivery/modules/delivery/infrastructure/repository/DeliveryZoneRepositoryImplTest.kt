package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.DeliveryZoneGeometryProjector
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryZoneEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryZoneJpaRepository
import java.time.Instant
import java.util.Optional
import java.util.UUID

class DeliveryZoneRepositoryImplTest {

    @Test
    fun `rebuilds effective geometries after polygon save`() {
        val jpaRepository = mock(DeliveryZoneJpaRepository::class.java)
        val projector = mock(DeliveryZoneGeometryProjector::class.java)
        val repository = DeliveryZoneRepositoryImpl(jpaRepository, projector)
        val zone = polygonZone()
        `when`(jpaRepository.findById(zone.id)).thenReturn(Optional.empty())
        doAnswer { invocation -> invocation.getArgument<DeliveryZoneEntity>(0) }
            .`when`(jpaRepository).save(any(DeliveryZoneEntity::class.java))

        repository.save(zone)

        verify(projector).rebuildEffectiveGeometries()
    }

    @Test
    fun `does not rebuild effective geometries after city save`() {
        val jpaRepository = mock(DeliveryZoneJpaRepository::class.java)
        val projector = mock(DeliveryZoneGeometryProjector::class.java)
        val repository = DeliveryZoneRepositoryImpl(jpaRepository, projector)
        val zone = polygonZone().copy(
            type = DeliveryZoneType.CITY,
            city = "Yekaterinburg",
            normalizedCity = "yekaterinburg",
            geometry = null,
        )
        `when`(jpaRepository.findById(zone.id)).thenReturn(Optional.empty())
        doAnswer { invocation -> invocation.getArgument<DeliveryZoneEntity>(0) }
            .`when`(jpaRepository).save(any(DeliveryZoneEntity::class.java))

        repository.save(zone)

        verifyNoInteractions(projector)
    }

    @Test
    fun `rebuilds effective geometries after polygon delete`() {
        val jpaRepository = mock(DeliveryZoneJpaRepository::class.java)
        val projector = mock(DeliveryZoneGeometryProjector::class.java)
        val repository = DeliveryZoneRepositoryImpl(jpaRepository, projector)
        val existingEntity = DeliveryZoneEntity(
            id = UUID.randomUUID(),
            code = "CENTER",
            name = "Center",
            type = DeliveryZoneType.POLYGON,
            city = null,
            normalizedCity = null,
            postalCode = null,
            geometry = null,
            effectiveGeometry = null,
            priority = 1,
            isActive = true,
            createdAt = Instant.parse("2026-04-02T00:00:00Z"),
            updatedAt = Instant.parse("2026-04-02T00:00:00Z"),
        )
        `when`(jpaRepository.findById(existingEntity.id)).thenReturn(Optional.of(existingEntity))

        repository.deleteById(existingEntity.id)

        verify(jpaRepository).deleteById(existingEntity.id)
        verify(projector).rebuildEffectiveGeometries()
    }

    private fun polygonZone(): DeliveryZone {
        return DeliveryZone(
            id = UUID.randomUUID(),
            code = "CENTER",
            name = "Center",
            type = DeliveryZoneType.POLYGON,
            city = null,
            normalizedCity = null,
            postalCode = null,
            geometry = null,
            priority = 1,
            active = true,
        )
    }
}
