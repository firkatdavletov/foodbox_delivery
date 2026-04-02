package ru.foodbox.delivery.modules.delivery.application

import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.PrecisionModel
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.checkout.domain.repository.CheckoutPaymentMethodRuleRepository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
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
    fun `deletes delivery zone when it has no linked tariffs`() {
        val zoneId = UUID.randomUUID()
        val zoneRepository = InMemoryDeliveryZoneRepository().apply {
            save(
                DeliveryZone(
                    id = zoneId,
                    code = "EKB",
                    name = "Yekaterinburg",
                    type = DeliveryZoneType.CITY,
                    city = "Yekaterinburg",
                    normalizedCity = "yekaterinburg",
                    postalCode = null,
                    geometry = null,
                    priority = 0,
                    active = true,
                )
            )
        }
        val tariffRepository = InMemoryDeliveryTariffRepository()
        val service = deliveryAdminService(
            zoneRepository = zoneRepository,
            tariffRepository = tariffRepository,
        )

        service.deleteZone(zoneId)

        assertEquals(null, zoneRepository.findById(zoneId))
    }

    @Test
    fun `rejects deleting delivery zone with linked tariffs`() {
        val zoneId = UUID.randomUUID()
        val zoneRepository = InMemoryDeliveryZoneRepository().apply {
            save(
                DeliveryZone(
                    id = zoneId,
                    code = "EKB",
                    name = "Yekaterinburg",
                    type = DeliveryZoneType.CITY,
                    city = "Yekaterinburg",
                    normalizedCity = "yekaterinburg",
                    postalCode = null,
                    geometry = null,
                    priority = 0,
                    active = true,
                )
            )
        }
        val tariffRepository = InMemoryDeliveryTariffRepository().apply {
            save(
                DeliveryTariff(
                    id = UUID.randomUUID(),
                    method = DeliveryMethodType.COURIER,
                    zone = zoneRepository.findById(zoneId),
                    available = true,
                    fixedPriceMinor = 300,
                    freeFromAmountMinor = null,
                    currency = "RUB",
                    estimatedDays = 1,
                )
            )
        }
        val service = deliveryAdminService(
            zoneRepository = zoneRepository,
            tariffRepository = tariffRepository,
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            service.deleteZone(zoneId)
        }

        assertEquals("Cannot delete delivery zone while tariffs are linked to it", exception.message)
    }

    @Test
    fun `deletes tariff by id`() {
        val tariff = DeliveryTariff(
            id = UUID.randomUUID(),
            method = DeliveryMethodType.PICKUP,
            zone = null,
            available = true,
            fixedPriceMinor = 0,
            freeFromAmountMinor = null,
            currency = "RUB",
            estimatedDays = 0,
        )
        val tariffRepository = InMemoryDeliveryTariffRepository().apply {
            save(tariff)
        }
        val service = deliveryAdminService(
            zoneRepository = InMemoryDeliveryZoneRepository(),
            tariffRepository = tariffRepository,
        )

        service.deleteTariff(tariff.id)

        assertEquals(null, tariffRepository.findById(tariff.id))
    }

    @Test
    fun `deletes pickup point by id`() {
        val pickupPoint = PickupPoint(
            id = UUID.randomUUID(),
            code = "pickup-1",
            name = "Pickup 1",
            address = DeliveryAddress(city = "Yekaterinburg", street = "Lenina", house = "1"),
            active = true,
        )
        val pickupPointRepository = InMemoryPickupPointRepository().apply {
            save(pickupPoint)
        }
        val service = deliveryAdminService(
            zoneRepository = InMemoryDeliveryZoneRepository(),
            pickupPointRepository = pickupPointRepository,
        )

        service.deletePickupPoint(pickupPoint.id)

        assertEquals(null, pickupPointRepository.findById(pickupPoint.id))
    }

    @Test
    fun `throws not found when deleting missing tariff`() {
        val service = deliveryAdminService(InMemoryDeliveryZoneRepository())

        assertFailsWith<NotFoundException> {
            service.deleteTariff(UUID.randomUUID())
        }
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

    @Test
    fun `rejects negative tariff delivery minutes`() {
        val zone = DeliveryZone(
            id = UUID.randomUUID(),
            code = "EKB",
            name = "Yekaterinburg",
            type = DeliveryZoneType.CITY,
            city = "Yekaterinburg",
            normalizedCity = "yekaterinburg",
            postalCode = null,
            geometry = null,
            priority = 0,
            active = true,
        )
        val zoneRepository = InMemoryDeliveryZoneRepository().apply {
            save(zone)
        }
        val service = deliveryAdminService(zoneRepository = zoneRepository)

        val exception = assertFailsWith<IllegalArgumentException> {
            service.upsertTariff(
                DeliveryTariff(
                    id = UUID.randomUUID(),
                    method = DeliveryMethodType.COURIER,
                    zone = zone,
                    available = true,
                    fixedPriceMinor = 300,
                    freeFromAmountMinor = null,
                    currency = "RUB",
                    estimatedDays = 0,
                    deliveryMinutes = -1,
                )
            )
        }

        assertEquals("deliveryMinutes must be non-negative", exception.message)
    }

    private fun deliveryAdminService(
        zoneRepository: DeliveryZoneRepository,
        tariffRepository: DeliveryTariffRepository = InMemoryDeliveryTariffRepository(),
        pickupPointRepository: PickupPointRepository = InMemoryPickupPointRepository(),
        geocoder: DeliveryAddressGeocoder = mock(DeliveryAddressGeocoder::class.java),
    ): DeliveryAdminService {
        return DeliveryAdminService(
            deliveryMethodSettingRepository = mock(DeliveryMethodSettingRepository::class.java),
            deliveryZoneRepository = zoneRepository,
            deliveryTariffRepository = tariffRepository,
            pickupPointRepository = pickupPointRepository,
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

        override fun deleteById(id: UUID) {
            zonesById.remove(id)
        }

        override fun findActiveByPoint(latitude: Double, longitude: Double): DeliveryZone? = null

        override fun findActiveByCity(city: String): DeliveryZone? = null

        override fun findActiveByPostalCode(postalCode: String): DeliveryZone? = null
    }

    private class InMemoryDeliveryTariffRepository : DeliveryTariffRepository {
        private val tariffsById = linkedMapOf<UUID, DeliveryTariff>()

        override fun findAll(): List<DeliveryTariff> = tariffsById.values.toList()

        override fun findById(id: UUID): DeliveryTariff? = tariffsById[id]

        override fun save(tariff: DeliveryTariff): DeliveryTariff {
            tariffsById[tariff.id] = tariff
            return tariff
        }

        override fun deleteById(id: UUID) {
            tariffsById.remove(id)
        }

        override fun findByMethodAndZone(method: DeliveryMethodType, zoneId: UUID?): DeliveryTariff? {
            return tariffsById.values.firstOrNull { it.method == method && it.zone?.id == zoneId }
        }

        override fun findDefaultByMethod(method: DeliveryMethodType): DeliveryTariff? {
            return tariffsById.values.firstOrNull { it.method == method && it.zone == null }
        }

        override fun existsByZoneId(zoneId: UUID): Boolean {
            return tariffsById.values.any { it.zone?.id == zoneId }
        }
    }

    private class InMemoryPickupPointRepository : PickupPointRepository {
        private val pickupPointsById = linkedMapOf<UUID, PickupPoint>()

        override fun findAll(): List<PickupPoint> = pickupPointsById.values.toList()

        override fun findAllByIsActive(isActive: Boolean): List<PickupPoint> {
            return pickupPointsById.values.filter { it.active == isActive }
        }

        override fun findById(id: UUID): PickupPoint? = pickupPointsById[id]

        override fun findByCode(code: String): PickupPoint? = pickupPointsById.values.firstOrNull { it.code == code }

        override fun save(point: PickupPoint): PickupPoint {
            pickupPointsById[point.id] = point
            return point
        }

        override fun deleteById(id: UUID) {
            pickupPointsById.remove(id)
        }

        override fun findActiveById(id: UUID): PickupPoint? = pickupPointsById[id]?.takeIf { it.active }

        override fun findAllActive(): List<PickupPoint> = pickupPointsById.values.filter { it.active }
    }
}
