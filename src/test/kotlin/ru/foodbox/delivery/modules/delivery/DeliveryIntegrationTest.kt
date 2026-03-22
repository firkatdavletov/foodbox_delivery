package ru.foodbox.delivery.modules.delivery

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.delivery.application.YandexDeliveryGateway

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var yandexDeliveryGateway: YandexDeliveryGateway

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
}
