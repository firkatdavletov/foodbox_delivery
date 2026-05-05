package ru.foodbox.delivery.modules.promotions

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromoCodeAdminIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.execute("delete from promo_code_redemptions")
        jdbcTemplate.execute("delete from promo_codes")
    }

    @Test
    fun `admin promo code endpoints require authentication`() {
        mockMvc.perform(get("/api/v1/admin/promo-codes"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin can create filter search update and delete promo code`() {
        val createResponse = mockMvc.perform(
            post("/api/v1/admin/promo-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "code": "spring30",
                      "discountType": "PERCENT",
                      "discountValue": 30,
                      "minOrderAmountMinor": 2000,
                      "maxDiscountMinor": 1500,
                      "currency": "rub",
                      "usageLimitTotal": 50,
                      "usageLimitPerUser": 2,
                      "active": true
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SPRING30"))
            .andExpect(jsonPath("$.discountType").value("PERCENT"))
            .andExpect(jsonPath("$.discountValue").value(30))
            .andExpect(jsonPath("$.currency").value("RUB"))
            .andReturn()

        val promoCodeId = objectMapper.readTree(createResponse.response.contentAsByteArray)["id"].asText()

        mockMvc.perform(get("/api/v1/admin/promo-codes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(
            get("/api/v1/admin/promo-codes")
                .queryParam("discountType", "PERCENT")
                .queryParam("active", "true")
                .queryParam("code", "spring")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(
            get("/api/v1/admin/promo-codes/search")
                .queryParam("code", "SPRING30")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(promoCodeId))

        mockMvc.perform(get("/api/v1/admin/promo-codes/{promoCodeId}", promoCodeId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SPRING30"))

        mockMvc.perform(
            put("/api/v1/admin/promo-codes/{promoCodeId}", promoCodeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "code": "spring20",
                      "discountType": "PERCENT",
                      "discountValue": 20,
                      "minOrderAmountMinor": 1000,
                      "maxDiscountMinor": 800,
                      "currency": "RUB",
                      "usageLimitTotal": 100,
                      "usageLimitPerUser": 3,
                      "active": false
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SPRING20"))
            .andExpect(jsonPath("$.discountValue").value(20))
            .andExpect(jsonPath("$.active").value(false))

        mockMvc.perform(delete("/api/v1/admin/promo-codes/{promoCodeId}", promoCodeId))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/v1/admin/promo-codes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}
