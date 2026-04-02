package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryZoneEntity
import java.util.UUID

interface DeliveryZoneJpaRepository : JpaRepository<DeliveryZoneEntity, UUID> {
    fun findAllByIsActiveOrderByNameAsc(isActive: Boolean): List<DeliveryZoneEntity>
    fun findByCode(code: String): DeliveryZoneEntity?
    fun findByNormalizedCityAndTypeAndIsActiveTrue(normalizedCity: String, type: DeliveryZoneType): DeliveryZoneEntity?
    fun findByPostalCodeAndTypeAndIsActiveTrue(postalCode: String, type: DeliveryZoneType): DeliveryZoneEntity?

    @Query(
        value = """
            select dz.*
            from delivery_zones dz
            where dz.is_active = true
              and dz.type = 'POLYGON'
              and dz.effective_geometry is not null
              and st_covers(dz.effective_geometry, st_setsrid(st_point(:longitude, :latitude), 4326))
            order by dz.priority asc, dz.updated_at desc, dz.id asc
            limit 1
        """,
        nativeQuery = true,
    )
    fun findActivePolygonContainingPoint(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
    ): DeliveryZoneEntity?
}
