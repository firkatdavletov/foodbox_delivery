package ru.foodbox.delivery.modules.delivery

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.delivery.application.DeliveryAddressGeocoder
import ru.foodbox.delivery.modules.delivery.application.YandexDeliveryGateway
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var yandexDeliveryGateway: YandexDeliveryGateway

    @MockBean
    private lateinit var deliveryAddressGeocoder: DeliveryAddressGeocoder

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
    @WithMockUser(roles = ["ADMIN"])
    fun `detects pickup point address for admin by coordinates`() {
        Mockito.`when`(deliveryAddressGeocoder.reverseGeocode(56.8389, 60.6057)).thenReturn(
            DeliveryAddress(
                country = "Россия",
                region = "Свердловская область",
                city = "Екатеринбург",
                street = "улица 8 Марта",
                house = "10",
                postalCode = "620014",
                latitude = 56.839,
                longitude = 60.606,
            )
        )

        mockMvc.perform(
            post("/api/v1/admin/delivery/pickup-points/address-detect")
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
    }
}
