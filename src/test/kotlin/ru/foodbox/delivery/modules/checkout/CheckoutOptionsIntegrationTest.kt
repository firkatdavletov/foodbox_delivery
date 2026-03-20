package ru.foodbox.delivery.modules.checkout

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutOptionsIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `returns public checkout options with payment methods per delivery method`() {
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
}
