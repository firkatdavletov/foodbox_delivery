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
    fun `returns public checkout options with payment methods per delivery method`() {
        Mockito.`when`(yandexDeliveryGateway.isConfigured()).thenReturn(false)

        mockMvc.perform(get("/api/v1/checkout/options"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.options.length()").value(2))
            .andExpect(jsonPath("$.options[0].code").value("PICKUP"))
            .andExpect(jsonPath("$.options[0].paymentMethods.length()").value(2))
            .andExpect(jsonPath("$.options[0].paymentMethods[0].code").value("CASH"))
            .andExpect(jsonPath("$.options[0].paymentMethods[1].code").value("CARD_ON_DELIVERY"))
            .andExpect(jsonPath("$.options[1].code").value("COURIER"))
            .andExpect(jsonPath("$.options[1].paymentMethods.length()").value(2))
            .andExpect(jsonPath("$.options[1].paymentMethods[0].code").value("CASH"))
            .andExpect(jsonPath("$.options[1].paymentMethods[1].code").value("CARD_ON_DELIVERY"))
    }

    @Test
    fun `returns yandex pickup point payment methods resolved from yandex response`() {
        Mockito.`when`(yandexDeliveryGateway.isConfigured()).thenReturn(true)
        Mockito.`when`(yandexDeliveryGateway.listPickupPoints(213L))
            .thenReturn(
                listOf(
                    YandexPickupPointOption(
                        id = "yandex-point-1",
                        name = "Yandex Point 1",
                        address = "Lenina, 1",
                        paymentMethods = listOf("already_paid", "card_on_receipt"),
                    )
                )
            )

        mockMvc.perform(get("/api/v1/checkout/options").queryParam("yandexGeoId", "213"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.options.length()").value(3))
            .andExpect(jsonPath("$.options[2].code").value("YANDEX_PICKUP_POINT"))
            .andExpect(jsonPath("$.options[2].paymentMethods.length()").value(1))
            .andExpect(jsonPath("$.options[2].paymentMethods[0].code").value("CARD_ON_DELIVERY"))
    }
}
