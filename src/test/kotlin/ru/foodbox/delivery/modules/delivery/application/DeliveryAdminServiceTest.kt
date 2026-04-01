package ru.foodbox.delivery.modules.delivery.application

import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.PrecisionModel
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.checkout.domain.repository.CheckoutPaymentMethodRuleRepository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryMethodSettingRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryTariffRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryZoneRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeliveryAdminServiceTest {

    @Test
    fun `detects pickup point address by coordinates using geocoder`() {
        val geocoder = mock(DeliveryAddressGeocoder::class.java)
        val service = deliveryAdminService(
            zoneRepository = InMemoryDeliveryZoneRepository(),
            geocoder = geocoder,
        )
        val expectedAddress = DeliveryAddress(
            city = "Yekaterinburg",
            street = "ulitsa 8 Marta",
            house = "10",
            latitude = 56.839,
            longitude = 60.606,
        )
        `when`(geocoder.reverseGeocode(56.8389, 60.6057)).thenReturn(expectedAddress)

        val actualAddress = service.detectPickupPointAddress(
            latitude = 56.8389,
            longitude = 60.6057,
        )

        assertEquals(expectedAddress, actualAddress)
        verify(geocoder).reverseGeocode(56.8389, 60.6057)
    }

    @Test
    fun `saves polygon zone without city and postal code`() {
        val zoneRepository = InMemoryDeliveryZoneRepository()
        val service = deliveryAdminService(zoneRepository)

        val saved = service.upsertZone(
            DeliveryZone(
                id = UUID.randomUUID(),
                code = "poly-ekb",
                name = "Polygon Zone",
                type = DeliveryZoneType.POLYGON,
                city = null,
                normalizedCity = null,
                postalCode = null,
                geometry = rectangleMultiPolygon(),
                priority = 5,
                active = true,
            )
        )

        assertEquals(DeliveryZoneType.POLYGON, saved.type)
        assertEquals(5, saved.priority)
        assertEquals(4326, saved.geometry?.srid)
    }

    @Test
    fun `rejects geometry for non polygon zone`() {
        val service = deliveryAdminService(InMemoryDeliveryZoneRepository())

        val exception = assertFailsWith<IllegalArgumentException> {
            service.upsertZone(
                DeliveryZone(
                    id = UUID.randomUUID(),
                    code = "city-ekb",
                    name = "City Zone",
                    type = DeliveryZoneType.CITY,
                    city = "Yekaterinburg",
                    normalizedCity = null,
                    postalCode = null,
                    geometry = rectangleMultiPolygon(),
                    priority = 0,
                    active = true,
                )
            )
        }

        assertEquals("geometry is supported only for POLYGON delivery zones", exception.message)
    }

    private fun deliveryAdminService(
        zoneRepository: DeliveryZoneRepository,
        geocoder: DeliveryAddressGeocoder = mock(DeliveryAddressGeocoder::class.java),
    ): DeliveryAdminService {
        return DeliveryAdminService(
            deliveryMethodSettingRepository = mock(DeliveryMethodSettingRepository::class.java),
            deliveryZoneRepository = zoneRepository,
            deliveryTariffRepository = mock(DeliveryTariffRepository::class.java),
            pickupPointRepository = mock(PickupPointRepository::class.java),
            checkoutPaymentMethodRuleRepository = mock(CheckoutPaymentMethodRuleRepository::class.java),
            deliveryAddressGeocoder = geocoder,
        )
    }

    private fun rectangleMultiPolygon(): MultiPolygon {
        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val shell = geometryFactory.createLinearRing(
            arrayOf(
                Coordinate(60.55, 56.80),
                Coordinate(60.70, 56.80),
                Coordinate(60.70, 56.92),
                Coordinate(60.55, 56.80),
            )
        )
        return geometryFactory.createMultiPolygon(
            arrayOf(geometryFactory.createPolygon(shell))
        ).also { geometry ->
            geometry.srid = 4326
        }
    }

    private class InMemoryDeliveryZoneRepository : DeliveryZoneRepository {
        private val zonesById = linkedMapOf<UUID, DeliveryZone>()

        override fun findAll(): List<DeliveryZone> = zonesById.values.toList()

        override fun findAllByIsActive(isActive: Boolean): List<DeliveryZone> {
            return zonesById.values.filter { it.active == isActive }
        }

        override fun findById(id: UUID): DeliveryZone? = zonesById[id]

        override fun findByCode(code: String): DeliveryZone? = zonesById.values.firstOrNull { it.code == code }

        override fun save(zone: DeliveryZone): DeliveryZone {
            zonesById[zone.id] = zone
            return zone
        }

        override fun findActiveByPoint(latitude: Double, longitude: Double): DeliveryZone? = null

        override fun findActiveByCity(city: String): DeliveryZone? = null

        override fun findActiveByPostalCode(postalCode: String): DeliveryZone? = null
    }
}
