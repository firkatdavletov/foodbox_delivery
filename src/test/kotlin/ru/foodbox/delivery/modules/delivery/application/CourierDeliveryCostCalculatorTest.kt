package ru.foodbox.delivery.modules.delivery.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryTariffRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryZoneRepository
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourierDeliveryCostCalculatorTest {

    @Test
    fun `returns free delivery when subtotal reaches free threshold`() {
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
        val calculator = CourierDeliveryCostCalculator(
            deliveryZoneRepository = StubDeliveryZoneRepository(zone),
            deliveryTariffRepository = StubDeliveryTariffRepository(
                DeliveryTariff(
                    id = UUID.randomUUID(),
                    method = DeliveryMethodType.COURIER,
                    zone = zone,
                    available = true,
                    fixedPriceMinor = 29_900,
                    freeFromAmountMinor = 300_000,
                    currency = "RUB",
                    estimatedDays = 1,
                    deliveryMinutes = 60,
                )
            ),
        )

        val quote = calculator.calculate(
            DeliveryQuoteContext(
                subtotalMinor = 300_000,
                itemCount = 2,
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryAddress = DeliveryAddress(
                    city = "Yekaterinburg",
                    street = "Lenina",
                    house = "1",
                ),
            )
        )

        assertTrue(quote.available)
        assertEquals(0L, quote.priceMinor)
        assertEquals("EKB", quote.zoneCode)
        assertEquals(60, quote.estimatesMinutes)
    }

    @Test
    fun `returns unavailable quote when address is outside configured zones`() {
        val calculator = CourierDeliveryCostCalculator(
            deliveryZoneRepository = StubDeliveryZoneRepository(null),
            deliveryTariffRepository = StubDeliveryTariffRepository(null),
        )

        val quote = calculator.calculate(
            DeliveryQuoteContext(
                subtotalMinor = 100_000,
                itemCount = 1,
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryAddress = DeliveryAddress(
                    city = "Perm",
                    street = "Kuybysheva",
                    house = "10",
                ),
            )
        )

        assertFalse(quote.available)
        assertEquals(null, quote.priceMinor)
    }

    @Test
    fun `calculates courier quote by coordinates without requiring textual address fields`() {
        val zone = DeliveryZone(
            id = UUID.randomUUID(),
            code = "EKB",
            name = "Yekaterinburg",
            type = DeliveryZoneType.POLYGON,
            city = null,
            normalizedCity = null,
            postalCode = null,
            geometry = null,
            priority = 0,
            active = true,
        )
        val calculator = CourierDeliveryCostCalculator(
            deliveryZoneRepository = StubDeliveryZoneRepository(zone),
            deliveryTariffRepository = StubDeliveryTariffRepository(
                DeliveryTariff(
                    id = UUID.randomUUID(),
                    method = DeliveryMethodType.COURIER,
                    zone = zone,
                    available = true,
                    fixedPriceMinor = 29_900,
                    freeFromAmountMinor = null,
                    currency = "RUB",
                    estimatedDays = 1,
                )
            ),
        )

        val quote = calculator.calculate(
            DeliveryQuoteContext(
                subtotalMinor = 100_000,
                itemCount = 1,
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryAddress = DeliveryAddress(
                    latitude = 56.8389,
                    longitude = 60.6057,
                ),
            )
        )

        assertTrue(quote.available)
        assertEquals(29_900L, quote.priceMinor)
        assertEquals("EKB", quote.zoneCode)
    }

    private class StubDeliveryZoneRepository(
        private val zone: DeliveryZone?,
    ) : DeliveryZoneRepository {
        override fun findAll(): List<DeliveryZone> = listOfNotNull(zone)
        override fun findAllByIsActive(isActive: Boolean): List<DeliveryZone> = listOfNotNull(zone?.takeIf { it.active == isActive })
        override fun findById(id: UUID): DeliveryZone? = zone?.takeIf { it.id == id }
        override fun findByCode(code: String): DeliveryZone? = zone?.takeIf { it.code == code }
        override fun save(zone: DeliveryZone): DeliveryZone = zone
        override fun deleteById(id: UUID) = Unit
        override fun findActiveByPoint(latitude: Double, longitude: Double): DeliveryZone? = zone
        override fun findActiveByCity(city: String): DeliveryZone? = zone
        override fun findActiveByPostalCode(postalCode: String): DeliveryZone? = zone
    }

    private class StubDeliveryTariffRepository(
        private val tariff: DeliveryTariff?,
    ) : DeliveryTariffRepository {
        override fun findAll(): List<DeliveryTariff> = listOfNotNull(tariff)
        override fun findById(id: UUID): DeliveryTariff? = tariff?.takeIf { it.id == id }
        override fun save(tariff: DeliveryTariff): DeliveryTariff = tariff
        override fun deleteById(id: UUID) = Unit
        override fun findByMethodAndZone(method: DeliveryMethodType, zoneId: UUID?): DeliveryTariff? = tariff
        override fun findDefaultByMethod(method: DeliveryMethodType): DeliveryTariff? = tariff
        override fun existsByZoneId(zoneId: UUID): Boolean = tariff?.zone?.id == zoneId
    }
}
