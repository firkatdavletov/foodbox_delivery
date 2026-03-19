package ru.foodbox.delivery.modules.delivery.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
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
            city = "Yekaterinburg",
            postalCode = null,
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

    private class StubDeliveryZoneRepository(
        private val zone: DeliveryZone?,
    ) : DeliveryZoneRepository {
        override fun findActiveByCity(city: String): DeliveryZone? = zone
        override fun findActiveByPostalCode(postalCode: String): DeliveryZone? = zone
    }

    private class StubDeliveryTariffRepository(
        private val tariff: DeliveryTariff?,
    ) : DeliveryTariffRepository {
        override fun findByMethodAndZone(method: DeliveryMethodType, zoneId: UUID?): DeliveryTariff? = tariff
        override fun findDefaultByMethod(method: DeliveryMethodType): DeliveryTariff? = tariff
    }
}
