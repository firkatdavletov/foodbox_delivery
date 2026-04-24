package ru.foodbox.delivery.modules.catalog

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogSearchIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var categoryRepository: CatalogCategoryRepository

    @Autowired
    private lateinit var productRepository: CatalogProductRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanup() {
        listOf(
            "catalog_product_variant_images",
            "catalog_product_images",
            "catalog_category_images",
            "product_popularity_stats",
            "product_modifier_groups",
            "modifier_options",
            "modifier_groups",
            "catalog_product_variant_option_values",
            "catalog_product_option_values",
            "catalog_product_variants",
            "catalog_product_option_groups",
            "catalog_products",
            "catalog_categories",
        ).forEach { tableName ->
            jdbcTemplate.execute("delete from $tableName")
        }
    }

    @Test
    fun `catalog product search matches cyrillic title ignoring case`() {
        val categoryId = createCategory()
        createProduct(categoryId = categoryId, title = "Манишка", slug = "manishka")
        createProduct(categoryId = categoryId, title = "Кашмау", slug = "kashmau")

        val response = mockMvc.perform(
            get("/api/v1/catalog/products")
                .param("query", "ман")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val titles = objectMapper.readTree(response).map { it.get("title").asText() }

        assertEquals(listOf("Манишка"), titles)
    }

    @Test
    fun `catalog product search matches cyrillic title ignoring case within category`() {
        val accessoriesCategoryId = createCategory(slug = "accessories")
        val decorationCategoryId = createCategory(slug = "decoration")
        createProduct(categoryId = accessoriesCategoryId, title = "Манишка", slug = "manishka")
        createProduct(categoryId = decorationCategoryId, title = "Манглай", slug = "manglay")

        val response = mockMvc.perform(
            get("/api/v1/catalog/products")
                .param("categoryId", accessoriesCategoryId.toString())
                .param("query", "ман")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val titles = objectMapper.readTree(response).map { it.get("title").asText() }

        assertEquals(listOf("Манишка"), titles)
    }

    @Test
    fun `catalog categories are limited by query parameter`() {
        repeat(101) { index ->
            createCategory(slug = "category-${(index + 1).toString().padStart(3, '0')}")
        }

        val limitedResponse = mockMvc.perform(
            get("/api/v1/catalog/categories")
                .param("limit", "2")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val limitedCategories = objectMapper.readTree(limitedResponse)
        val limitedNames = limitedCategories.map { it.get("name").asText() }
        assertEquals(listOf("category-001", "category-002"), limitedNames)

        val defaultResponse = mockMvc.perform(
            get("/api/v1/catalog/categories")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        assertEquals(100, objectMapper.readTree(defaultResponse).size())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `popular products are returned by manual product stats score`() {
        val categoryId = createCategory()
        val lowScoreProductId = createProduct(categoryId = categoryId, title = "Low score", slug = "low-score")
        val highScoreProductId = createProduct(categoryId = categoryId, title = "High score", slug = "high-score")
        val disabledProductId = createProduct(categoryId = categoryId, title = "Disabled", slug = "disabled")

        upsertPopularity(lowScoreProductId, enabled = true, manualScore = 50)
        upsertPopularity(highScoreProductId, enabled = true, manualScore = 100)
        upsertPopularity(disabledProductId, enabled = false, manualScore = 1000)

        val response = mockMvc.perform(
            get("/api/v1/catalog/products/popular")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val titles = objectMapper.readTree(response).map { it.get("title").asText() }
        assertEquals(listOf("High score", "Low score"), titles)

        val statsResponse = mockMvc.perform(
            get("/api/v1/admin/product-stats/popularity/{productId}", lowScoreProductId)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val stats = objectMapper.readTree(statsResponse)
        assertEquals(lowScoreProductId.toString(), stats.get("productId").asText())
        assertEquals(true, stats.get("enabled").asBoolean())
        assertEquals(50, stats.get("manualScore").asInt())

        val adminListResponse = mockMvc.perform(
            get("/api/v1/admin/product-stats/popularity")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val adminItems = objectMapper.readTree(adminListResponse)
        assertEquals(listOf("High score", "Low score"), adminItems.map { it.get("product").get("title").asText() })
        assertEquals(listOf(true, true), adminItems.map { it.get("enabled").asBoolean() })
        assertEquals(listOf(100, 50), adminItems.map { it.get("manualScore").asInt() })

        mockMvc.perform(
            put("/api/v1/admin/product-stats/popularity/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "productIds" to listOf(lowScoreProductId, highScoreProductId),
                        )
                    )
                )
        )
            .andExpect(status().isOk)

        val reorderedResponse = mockMvc.perform(
            get("/api/v1/catalog/products/popular")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        assertEquals(
            listOf("Low score", "High score"),
            objectMapper.readTree(reorderedResponse).map { it.get("title").asText() },
        )
    }

    private fun createCategory(slug: String = "category"): UUID {
        val now = Instant.now()
        val category = categoryRepository.save(
            CatalogCategory(
                id = UUID.randomUUID(),
                name = slug,
                slug = slug,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )
        return category.id
    }

    private fun createProduct(categoryId: UUID, title: String, slug: String): UUID {
        val now = Instant.now()
        val product = productRepository.save(
            CatalogProduct(
                id = UUID.randomUUID(),
                categoryId = categoryId,
                title = title,
                slug = slug,
                description = null,
                priceMinor = 1_000,
                oldPriceMinor = null,
                sku = null,
                unit = ProductUnit.PIECE,
                countStep = 1,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )
        return product.id
    }

    private fun upsertPopularity(productId: UUID, enabled: Boolean, manualScore: Int) {
        mockMvc.perform(
            put("/api/v1/admin/product-stats/popularity/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "enabled" to enabled,
                            "manualScore" to manualScore,
                        )
                    )
                )
        )
            .andExpect(status().isOk)
    }
}
