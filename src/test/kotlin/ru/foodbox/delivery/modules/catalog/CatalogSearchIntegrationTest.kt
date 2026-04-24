package ru.foodbox.delivery.modules.catalog

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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
}
