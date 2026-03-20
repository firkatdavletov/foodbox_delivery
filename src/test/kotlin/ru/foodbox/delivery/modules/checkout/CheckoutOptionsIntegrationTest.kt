package ru.foodbox.delivery.modules.checkout

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
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutOptionsIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var yandexDeliveryGateway: YandexDeliveryGateway

    @Test
    fun `returns no checkout options when yandex pickup point is not selected`() {
        Mockito.`when`(yandexDeliveryGateway.isConfigured()).thenReturn(true)

        mockMvc.perform(get("/api/v1/checkout/options"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.options.length()").value(0))
    }

    @Test
    fun `returns yandex pickup point payment methods resolved from yandex response`() {
        Mockito.`when`(yandexDeliveryGateway.isConfigured()).thenReturn(true)
        Mockito.`when`(yandexDeliveryGateway.getPickupPoint("yandex-point-1"))
            .thenReturn(
                YandexPickupPointOption(
                    id = "yandex-point-1",
                    name = "Yandex Point 1",
                    address = "Lenina, 1",
                    paymentMethods = listOf("already_paid", "card_on_receipt"),
                )
            )

        mockMvc.perform(get("/api/v1/checkout/options").queryParam("pickupPointId", "yandex-point-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.options.length()").value(1))
            .andExpect(jsonPath("$.options[0].code").value("YANDEX_PICKUP_POINT"))
            .andExpect(jsonPath("$.options[0].paymentMethods.length()").value(1))
            .andExpect(jsonPath("$.options[0].paymentMethods[0].code").value("CARD_ON_DELIVERY"))
    }
}
