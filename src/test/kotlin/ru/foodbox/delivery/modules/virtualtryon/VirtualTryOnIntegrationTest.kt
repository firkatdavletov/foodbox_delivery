package ru.foodbox.delivery.modules.virtualtryon

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogCategoryJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantJpaRepository
import ru.foodbox.delivery.modules.virtualtryon.application.StartVirtualTryOnProviderResponse
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnProviderGateway
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnProviderStatusResponse
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnUpdatePublisher
import ru.foodbox.delivery.modules.virtualtryon.infrastructure.persistence.jpa.VirtualTryOnSessionJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VirtualTryOnIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var categoryRepository: CatalogCategoryRepository

    @Autowired
    private lateinit var productRepository: CatalogProductRepository

    @Autowired
    private lateinit var virtualTryOnSessionJpaRepository: VirtualTryOnSessionJpaRepository

    @Autowired
    private lateinit var variantJpaRepository: CatalogProductVariantJpaRepository

    @Autowired
    private lateinit var productJpaRepository: CatalogProductJpaRepository

    @Autowired
    private lateinit var categoryJpaRepository: CatalogCategoryJpaRepository

    @MockBean
    private lateinit var providerGateway: VirtualTryOnProviderGateway

    @MockBean
    private lateinit var updatePublisher: VirtualTryOnUpdatePublisher

    @BeforeEach
    fun cleanup() {
        virtualTryOnSessionJpaRepository.deleteAllInBatch()
        variantJpaRepository.deleteAllInBatch()
        productJpaRepository.deleteAllInBatch()
        categoryJpaRepository.deleteAllInBatch()
        Mockito.reset(providerGateway, updatePublisher)
    }

    @Test
    fun `create virtual try-on session for guest`() {
        val product = createProduct(imageUrl = "https://cdn.example.com/products/dress.jpg")

        Mockito.doReturn(StartVirtualTryOnProviderResponse(predictionId = "pred-100"))
            .`when`(providerGateway)
            .startTryOn(anyObject())

        val response = mockMvc.perform(
            post("/api/v1/virtual-try-on/sessions")
                .header("X-Install-Id", "install-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productId": "$product",
                      "modelImage": "https://cdn.example.com/models/model-1.jpg",
                      "mode": "balanced",
                      "outputFormat": "jpeg"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(response.response.contentAsByteArray)
        assertEquals("pending", body["status"].asText())
        assertEquals("starting", body["providerStatus"].asText())
        assertEquals("https://cdn.example.com/products/dress.jpg", body["garmentImageUrl"].asText())
        assertEquals("/ws/virtual-try-on", body["websocketEndpoint"].asText())
        assertNotNull(body["id"]?.asText())
        assertNotNull(body["websocketDestination"]?.asText())

        Mockito.verify(providerGateway).startTryOn(anyObject())
        Mockito.verifyNoInteractions(updatePublisher)
    }

    @Test
    fun `webhook completes virtual try-on session and publish update`() {
        val sessionId = createSession()

        mockMvc.perform(
            post("/api/v1/virtual-try-on/webhooks/fashn")
                .queryParam("token", "test-webhook-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "id": "pred-100",
                      "status": "completed",
                      "output": ["https://cdn.example.com/try-on/output-1.png"]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isNoContent)

        val session = getSession(sessionId)
        assertEquals("completed", session["status"].asText())
        assertEquals("completed", session["providerStatus"].asText())
        assertEquals(
            "https://cdn.example.com/try-on/output-1.png",
            session["outputImages"][0].asText(),
        )

        Mockito.verify(updatePublisher).publish(anyObject())
    }

    private fun createSession(): UUID {
        val product = createProduct(imageUrl = "https://cdn.example.com/products/dress.jpg")

        Mockito.doReturn(StartVirtualTryOnProviderResponse(predictionId = "pred-100"))
            .`when`(providerGateway)
            .startTryOn(anyObject())
        Mockito.`when`(providerGateway.getPredictionStatus("pred-100"))
            .thenReturn(
                VirtualTryOnProviderStatusResponse(
                    predictionId = "pred-100",
                    providerStatus = "processing",
                    outputImages = emptyList(),
                    errorName = null,
                    errorMessage = null,
                )
            )

        val response = mockMvc.perform(
            post("/api/v1/virtual-try-on/sessions")
                .header("X-Install-Id", "install-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productId": "$product",
                      "modelImage": "https://cdn.example.com/models/model-1.jpg"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()

        return UUID.fromString(objectMapper.readTree(response.response.contentAsByteArray)["id"].asText())
    }

    private fun getSession(sessionId: UUID): JsonNode {
        val response = mockMvc.perform(
            get("/api/v1/virtual-try-on/sessions/{sessionId}", sessionId)
                .header("X-Install-Id", "install-123")
        )
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(response.response.contentAsByteArray)
    }

    private fun createProduct(imageUrl: String): UUID {
        val now = Instant.now()
        val category = categoryRepository.save(
            CatalogCategory(
                id = UUID.randomUUID(),
                name = "Dresses",
                slug = "dresses",
                imageUrl = null,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )

        return productRepository.save(
            CatalogProduct(
                id = UUID.randomUUID(),
                categoryId = category.id,
                title = "Summer Dress",
                slug = "summer-dress",
                description = null,
                priceMinor = 10_000,
                oldPriceMinor = null,
                sku = "DRESS-001",
                imageUrl = imageUrl,
                unit = ProductUnit.PIECE,
                countStep = 1,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        ).id
    }

    private fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
