package ru.foodbox.delivery.modules.delivery

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.cart.application.CartService
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryDraft
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryQuote
import ru.foodbox.delivery.modules.delivery.application.YandexDeliveryGateway
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository
import ru.foodbox.delivery.common.web.CurrentActor
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var yandexDeliveryGateway: YandexDeliveryGateway

    @MockBean
    private lateinit var cartService: CartService

    @MockBean
    private lateinit var pickupPointRepository: PickupPointRepository

    @Test
    fun `returns public delivery methods without authentication`() {
        Mockito.`when`(yandexDeliveryGateway.isConfigured()).thenReturn(false)

        mockMvc.perform(get("/api/v1/delivery/methods"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.methods.length()").value(0))
            .andExpect(jsonPath("$.pickupPoints.length()").value(0))
    }

    @Test
    fun `returns only yandex pickup point method when yandex delivery is configured`() {
        Mockito.`when`(yandexDeliveryGateway.isConfigured()).thenReturn(true)

        mockMvc.perform(get("/api/v1/delivery/methods"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.methods.length()").value(1))
            .andExpect(jsonPath("$.methods[0].code").value("YANDEX_PICKUP_POINT"))
            .andExpect(jsonPath("$.pickupPoints.length()").value(0))
    }

    @Test
    fun `returns active pickup points`() {
        Mockito.`when`(pickupPointRepository.findAllActive()).thenReturn(
            listOf(
                PickupPoint(
                    id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    code = "pickup-1",
                    name = "Main Pickup Point",
                    address = DeliveryAddress(
                        city = "Екатеринбург",
                        street = "Ленина",
                        house = "10",
                        latitude = 56.8389,
                        longitude = 60.6057,
                    ),
                    active = true,
                )
            )
        )

        mockMvc.perform(get("/api/v1/delivery/pickup-points"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pickupPoints.length()").value(1))
            .andExpect(jsonPath("$.pickupPoints[0].id").value("11111111-1111-1111-1111-111111111111"))
            .andExpect(jsonPath("$.pickupPoints[0].code").value("pickup-1"))
            .andExpect(jsonPath("$.pickupPoints[0].name").value("Main Pickup Point"))
            .andExpect(jsonPath("$.pickupPoints[0].address.city").value("Екатеринбург"))
            .andExpect(jsonPath("$.pickupPoints[0].address.street").value("Ленина"))
            .andExpect(jsonPath("$.pickupPoints[0].address.house").value("10"))
            .andExpect(jsonPath("$.pickupPoints[0].address.latitude").value(56.8389))
            .andExpect(jsonPath("$.pickupPoints[0].address.longitude").value(60.6057))
            .andExpect(jsonPath("$.pickupPoints[0].isActive").value(true))
    }

    @Test
    fun `detects courier cart delivery draft by coordinates`() {
        Mockito.`when`(
            cartService.detectCourierDeliveryDraft(
                CurrentActor.Guest("install-1"),
                56.8389,
                60.6057,
            )
        ).thenReturn(
            CartDeliveryDraft(
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryAddress = DeliveryAddress(
                    country = "Россия",
                    region = "Свердловская область",
                    city = "Екатеринбург",
                    street = "улица 8 Марта",
                    house = "10",
                    postalCode = "620014",
                    latitude = 56.839,
                    longitude = 60.606,
                ),
                pickupPointId = null,
                pickupPointExternalId = null,
                pickupPointName = null,
                pickupPointAddress = null,
                quote = CartDeliveryQuote(
                    available = true,
                    priceMinor = 500,
                    currency = "RUB",
                    zoneCode = "EKB",
                    zoneName = "Yekaterinburg",
                    estimatedDays = 1,
                    estimatesMinutes = 60,
                    message = null,
                    calculatedAt = Instant.parse("2026-04-01T06:00:00Z"),
                    expiresAt = Instant.parse("2026-04-01T06:15:00Z"),
                ),
                createdAt = Instant.parse("2026-04-01T06:00:00Z"),
                updatedAt = Instant.parse("2026-04-01T06:00:00Z"),
            )
        )

        mockMvc.perform(
            post("/api/v1/delivery/courier/draft-detect")
                .header("X-Install-Id", "install-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"latitude":56.8389,"longitude":60.6057}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.address.country").value("Россия"))
            .andExpect(jsonPath("$.address.region").value("Свердловская область"))
            .andExpect(jsonPath("$.address.city").value("Екатеринбург"))
            .andExpect(jsonPath("$.address.street").value("улица 8 Марта"))
            .andExpect(jsonPath("$.address.house").value("10"))
            .andExpect(jsonPath("$.address.postalCode").value("620014"))
            .andExpect(jsonPath("$.address.latitude").value(56.839))
            .andExpect(jsonPath("$.address.longitude").value(60.606))
            .andExpect(jsonPath("$.deliveryMethod").value("COURIER"))
            .andExpect(jsonPath("$.quote.available").value(true))
            .andExpect(jsonPath("$.quote.priceMinor").value(500))
            .andExpect(jsonPath("$.quote.zoneCode").value("EKB"))
            .andExpect(jsonPath("$.quote.zoneName").value("Yekaterinburg"))
            .andExpect(jsonPath("$.quote.estimatesMinutes").value(60))
    }
}
