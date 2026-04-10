package ru.foodbox.delivery.modules.legal

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalDocumentIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `returns seeded legal document publicly without authentication`() {
        mockMvc.perform(get("/api/v1/public/legal-documents/public-offer"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("public-offer"))
            .andExpect(jsonPath("$.title").value("ПУБЛИЧНАЯ ОФЕРТА о продаже товаров дистанционным способом"))
            .andExpect(jsonPath("$.text").value(""))
    }

    @Test
    fun `admin legal endpoints require authentication`() {
        mockMvc.perform(get("/api/v1/admin/legal-documents"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin can list legal documents`() {
        mockMvc.perform(get("/api/v1/admin/legal-documents"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].type").value("public-offer"))
            .andExpect(jsonPath("$[1].type").value("personal-data-consent"))
            .andExpect(jsonPath("$[2].type").value("personal-data-policy"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin can update legal document and public endpoint returns saved content`() {
        mockMvc.perform(
            put("/api/v1/admin/legal-documents/personal-data-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Политика обработки персональных данных",
                      "subtitle": "Редакция от 10.04.2026",
                      "text": "Раздел 1. Общие положения"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("personal-data-policy"))
            .andExpect(jsonPath("$.title").value("Политика обработки персональных данных"))
            .andExpect(jsonPath("$.subtitle").value("Редакция от 10.04.2026"))
            .andExpect(jsonPath("$.text").value("Раздел 1. Общие положения"))

        mockMvc.perform(get("/api/v1/public/legal-documents/personal-data-policy"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("personal-data-policy"))
            .andExpect(jsonPath("$.title").value("Политика обработки персональных данных"))
            .andExpect(jsonPath("$.subtitle").value("Редакция от 10.04.2026"))
            .andExpect(jsonPath("$.text").value("Раздел 1. Общие положения"))
    }
}
