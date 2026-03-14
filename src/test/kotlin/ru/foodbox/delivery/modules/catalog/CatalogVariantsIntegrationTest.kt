package ru.foodbox.delivery.modules.catalog

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogCategoryJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductOptionGroupJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductOptionValueJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantOptionValueJpaRepository
import ru.foodbox.delivery.modules.catalogimport.application.CatalogImportService
import ru.foodbox.delivery.modules.catalogimport.application.command.ExecuteCatalogImportCommand
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogVariantsIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var categoryRepository: CatalogCategoryRepository

    @Autowired
    private lateinit var productRepository: CatalogProductRepository

    @Autowired
    private lateinit var catalogService: CatalogService

    @Autowired
    private lateinit var catalogImportService: CatalogImportService

    @Autowired
    private lateinit var variantOptionValueJpaRepository: CatalogProductVariantOptionValueJpaRepository

    @Autowired
    private lateinit var optionValueJpaRepository: CatalogProductOptionValueJpaRepository

    @Autowired
    private lateinit var variantJpaRepository: CatalogProductVariantJpaRepository

    @Autowired
    private lateinit var optionGroupJpaRepository: CatalogProductOptionGroupJpaRepository

    @Autowired
    private lateinit var productJpaRepository: CatalogProductJpaRepository

    @Autowired
    private lateinit var categoryJpaRepository: CatalogCategoryJpaRepository

    @BeforeEach
    fun cleanup() {
        variantOptionValueJpaRepository.deleteAllInBatch()
        optionValueJpaRepository.deleteAllInBatch()
        variantJpaRepository.deleteAllInBatch()
        optionGroupJpaRepository.deleteAllInBatch()
        productJpaRepository.deleteAllInBatch()
        categoryJpaRepository.deleteAllInBatch()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `create product with variants`() {
        val categoryId = createCategory(externalId = "cat-shirts", slug = "shirts")

        val request = mapOf(
            "categoryId" to categoryId,
            "title" to "Футболка",
            "priceMinor" to 3000,
            "oldPriceMinor" to 3500,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "color",
                    "title" to "Цвет",
                    "sortOrder" to 0,
                    "values" to listOf(
                        mapOf("code" to "black", "title" to "Черный", "sortOrder" to 0),
                        mapOf("code" to "white", "title" to "Белый", "sortOrder" to 1),
                    ),
                ),
                mapOf(
                    "code" to "size",
                    "title" to "Размер",
                    "sortOrder" to 1,
                    "values" to listOf(
                        mapOf("code" to "s", "title" to "S", "sortOrder" to 0),
                        mapOf("code" to "m", "title" to "M", "sortOrder" to 1),
                    ),
                ),
            ),
            "variants" to listOf(
                mapOf(
                    "externalId" to "var-1",
                    "sku" to "TSHIRT-BLACK-S",
                    "title" to "Черный S",
                    "priceMinor" to 3000,
                    "sortOrder" to 10,
                    "isActive" to true,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "color", "optionValueCode" to "black"),
                        mapOf("optionGroupCode" to "size", "optionValueCode" to "s"),
                    ),
                ),
                mapOf(
                    "externalId" to "var-2",
                    "sku" to "TSHIRT-WHITE-M",
                    "title" to "Белый M",
                    "priceMinor" to 3200,
                    "sortOrder" to 20,
                    "isActive" to true,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "color", "optionValueCode" to "white"),
                        mapOf("optionGroupCode" to "size", "optionValueCode" to "m"),
                    ),
                ),
            ),
        )

        val upsertResponse = upsertProductAsAdmin(request)
        val productId = UUID.fromString(upsertResponse.get("id").asText())

        val details = getProductDetails(productId)
        assertEquals(2, details.get("optionGroups").size())
        assertEquals(2, details.get("variants").size())
        assertNotNull(details.get("defaultVariantId")?.asText())
        assertEquals("TSHIRT-BLACK-S", details.get("variants")[0].get("sku").asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `update product replaces all variants`() {
        val categoryId = createCategory(externalId = "cat-jeans", slug = "jeans")

        val initialRequest = mapOf(
            "categoryId" to categoryId,
            "title" to "Джинсы",
            "priceMinor" to 5000,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "size",
                    "title" to "Размер",
                    "values" to listOf(
                        mapOf("code" to "m", "title" to "M"),
                    ),
                )
            ),
            "variants" to listOf(
                mapOf(
                    "sku" to "JEANS-M",
                    "title" to "M",
                    "isActive" to true,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "size", "optionValueCode" to "m"),
                    ),
                )
            ),
        )

        val created = upsertProductAsAdmin(initialRequest)
        val productId = UUID.fromString(created.get("id").asText())

        val updateRequest = mapOf(
            "id" to productId,
            "categoryId" to categoryId,
            "title" to "Джинсы",
            "priceMinor" to 5100,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "color",
                    "title" to "Цвет",
                    "values" to listOf(
                        mapOf("code" to "blue", "title" to "Синий"),
                    ),
                )
            ),
            "variants" to listOf(
                mapOf(
                    "sku" to "JEANS-BLUE",
                    "title" to "Синий",
                    "isActive" to true,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "color", "optionValueCode" to "blue"),
                    ),
                )
            ),
        )

        upsertProductAsAdmin(updateRequest)

        val details = getProductDetails(productId)
        assertEquals(1, details.get("optionGroups").size())
        assertEquals("color", details.get("optionGroups")[0].get("code").asText())
        assertEquals(1, details.get("variants").size())
        assertEquals("JEANS-BLUE", details.get("variants")[0].get("sku").asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `update product with same option group code and extra variant succeeds`() {
        val categoryId = createCategory(externalId = "cat-repeat-group-code", slug = "repeat-group-code")

        val initialRequest = mapOf(
            "categoryId" to categoryId,
            "title" to "Брюки",
            "priceMinor" to 5000,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "size",
                    "title" to "Размер",
                    "values" to listOf(
                        mapOf("code" to "m", "title" to "M"),
                    ),
                )
            ),
            "variants" to listOf(
                mapOf(
                    "sku" to "PANTS-M",
                    "title" to "M",
                    "isActive" to true,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "size", "optionValueCode" to "m"),
                    ),
                )
            ),
        )

        val created = upsertProductAsAdmin(initialRequest)
        val productId = UUID.fromString(created.get("id").asText())

        val updateRequest = mapOf(
            "id" to productId,
            "categoryId" to categoryId,
            "title" to "Брюки",
            "priceMinor" to 5100,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "size",
                    "title" to "Размер",
                    "values" to listOf(
                        mapOf("code" to "m", "title" to "M"),
                        mapOf("code" to "l", "title" to "L"),
                    ),
                )
            ),
            "variants" to listOf(
                mapOf(
                    "sku" to "PANTS-M",
                    "title" to "M",
                    "isActive" to true,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "size", "optionValueCode" to "m"),
                    ),
                ),
                mapOf(
                    "sku" to "PANTS-L",
                    "title" to "L",
                    "isActive" to true,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "size", "optionValueCode" to "l"),
                    ),
                ),
            ),
        )

        upsertProductAsAdmin(updateRequest)

        val details = getProductDetails(productId)
        assertEquals(1, details.get("optionGroups").size())
        assertEquals("size", details.get("optionGroups")[0].get("code").asText())
        assertEquals(2, details.get("variants").size())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `details api returns empty options for simple product`() {
        val categoryId = createCategory(externalId = "cat-simple", slug = "simple")

        val request = mapOf(
            "categoryId" to categoryId,
            "title" to "Базовый товар",
            "priceMinor" to 1200,
            "sku" to "SIMPLE-1",
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
        )

        val created = upsertProductAsAdmin(request)
        val productId = UUID.fromString(created.get("id").asText())

        val details = getProductDetails(productId)
        assertEquals(0, details.get("optionGroups").size())
        assertEquals(0, details.get("variants").size())
        assertTrue(details.get("defaultVariantId").isNull)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `details api returns variants and defaultVariantId`() {
        val categoryId = createCategory(externalId = "cat-sneakers", slug = "sneakers")

        val request = mapOf(
            "categoryId" to categoryId,
            "title" to "Кроссовки",
            "priceMinor" to 7000,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "size",
                    "title" to "Размер",
                    "values" to listOf(
                        mapOf("code" to "42", "title" to "42"),
                        mapOf("code" to "43", "title" to "43"),
                    ),
                )
            ),
            "variants" to listOf(
                mapOf(
                    "sku" to "SNKR-42",
                    "sortOrder" to 0,
                    "isActive" to false,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "size", "optionValueCode" to "42"),
                    ),
                ),
                mapOf(
                    "sku" to "SNKR-43",
                    "sortOrder" to 1,
                    "isActive" to true,
                    "options" to listOf(
                        mapOf("optionGroupCode" to "size", "optionValueCode" to "43"),
                    ),
                ),
            ),
        )

        val created = upsertProductAsAdmin(request)
        val productId = UUID.fromString(created.get("id").asText())

        val details = getProductDetails(productId)
        assertEquals(2, details.get("variants").size())
        val defaultVariantId = details.get("defaultVariantId").asText()
        val activeVariantId = details.get("variants")[1].get("id").asText()
        assertEquals(activeVariantId, defaultVariantId)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin product details endpoint returns inactive product`() {
        val categoryId = createCategory(externalId = "cat-admin-details", slug = "admin-details")

        val request = mapOf(
            "categoryId" to categoryId,
            "title" to "Скрытый товар",
            "priceMinor" to 1100,
            "sku" to "HIDDEN-1",
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to false,
        )

        val created = upsertProductAsAdmin(request)
        val productId = UUID.fromString(created.get("id").asText())

        mockMvc.perform(
            get("/api/v1/catalog/products/{productId}", productId)
        ).andExpect(status().isNotFound)

        val adminDetails = getAdminProductDetails(productId)
        assertEquals(productId.toString(), adminDetails.get("id").asText())
        assertEquals(false, adminDetails.get("isActive").asBoolean())
    }

    @Test
    fun `import simple product`() {
        createCategory(externalId = "cat-import-simple", slug = "import-simple")

        val csv = """
            external_id,sku,name,slug,description,category_external_id,price,old_price,brand,is_active,image_url,sort_order
            prd-simple-1,SIMPLE-CSV-1,Импорт простой,import-simple-1,Описание,cat-import-simple,99.90,,Brand,true,https://example.com/simple.jpg,0
        """.trimIndent()

        val report = catalogImportService.execute(
            ExecuteCatalogImportCommand(
                importType = CatalogImportType.PRODUCT,
                importMode = CatalogImportMode.UPSERT,
                csvBytes = csv.toByteArray(),
            )
        )

        assertEquals(0, report.errorCount)
        assertEquals(1, report.successCount)
        val product = productRepository.findByExternalId("prd-simple-1")
        assertNotNull(product)
        assertEquals("SIMPLE-CSV-1", product.sku)

        val details = catalogService.getProductDetails(product.id)
        assertNotNull(details)
        assertTrue(details.optionGroups.isEmpty())
        assertTrue(details.variants.isEmpty())
    }

    @Test
    fun `import product with variants`() {
        createCategory(externalId = "cat-import-var", slug = "import-var")

        val csv = """
            product_external_id,product_slug,product_title,category_external_id,product_description,product_brand,product_image_url,product_price_minor,product_old_price_minor,product_unit,product_count_step,product_is_active,variant_external_id,variant_sku,variant_title,variant_price_minor,variant_old_price_minor,variant_image_url,variant_sort_order,variant_is_active,option1_group_code,option1_group_title,option1_value_code,option1_value_title,option2_group_code,option2_group_title,option2_value_code,option2_value_title
            prd-shirt-1,shirt-1,Футболка,cat-import-var,Описание,Brand,https://example.com/shirt.jpg,2500,3000,PIECE,1,true,var-1,SHIRT-BLACK-S,Черная S,2500,3000,https://example.com/black-s.jpg,0,true,color,Цвет,black,Черный,size,Размер,s,S
            prd-shirt-1,shirt-1,Футболка,cat-import-var,Описание,Brand,https://example.com/shirt.jpg,2500,3000,PIECE,1,true,var-2,SHIRT-WHITE-M,Белая M,2600,3000,https://example.com/white-m.jpg,1,true,color,Цвет,white,Белый,size,Размер,m,M
        """.trimIndent()

        val report = catalogImportService.execute(
            ExecuteCatalogImportCommand(
                importType = CatalogImportType.PRODUCT,
                importMode = CatalogImportMode.UPSERT,
                csvBytes = csv.toByteArray(),
            )
        )

        assertEquals(0, report.errorCount)
        assertEquals(2, report.successCount)

        val product = productRepository.findByExternalId("prd-shirt-1")
        assertNotNull(product)

        val details = catalogService.getProductDetails(product.id)
        assertNotNull(details)
        assertEquals(2, details.optionGroups.size)
        assertEquals(2, details.variants.size)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `duplicate option group code returns validation error`() {
        val categoryId = createCategory(externalId = "cat-dup-group", slug = "dup-group")

        val request = mapOf(
            "categoryId" to categoryId,
            "title" to "Товар",
            "priceMinor" to 1000,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "color",
                    "title" to "Цвет",
                    "values" to listOf(mapOf("code" to "black", "title" to "Черный")),
                ),
                mapOf(
                    "code" to "color",
                    "title" to "Цвет 2",
                    "values" to listOf(mapOf("code" to "white", "title" to "Белый")),
                ),
            ),
            "variants" to listOf(
                mapOf(
                    "sku" to "DUP-GROUP-1",
                    "options" to listOf(mapOf("optionGroupCode" to "color", "optionValueCode" to "black")),
                )
            ),
        )

        val errorBody = upsertProductAsAdminExpectBadRequest(request)
        assertTrue(errorBody.contains("Duplicate option group code"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `duplicate variant sku returns validation error`() {
        val categoryId = createCategory(externalId = "cat-dup-sku", slug = "dup-sku")

        upsertProductAsAdmin(
            mapOf(
                "categoryId" to categoryId,
                "title" to "Первый",
                "priceMinor" to 1000,
                "unit" to "PIECE",
                "countStep" to 1,
                "isActive" to true,
                "optionGroups" to listOf(
                    mapOf(
                        "code" to "size",
                        "title" to "Размер",
                        "values" to listOf(mapOf("code" to "m", "title" to "M")),
                    )
                ),
                "variants" to listOf(
                    mapOf(
                        "sku" to "DUP-SKU-1",
                        "options" to listOf(mapOf("optionGroupCode" to "size", "optionValueCode" to "m")),
                    )
                ),
            )
        )

        val errorBody = upsertProductAsAdminExpectBadRequest(
            mapOf(
                "categoryId" to categoryId,
                "title" to "Второй",
                "priceMinor" to 1000,
                "unit" to "PIECE",
                "countStep" to 1,
                "isActive" to true,
                "optionGroups" to listOf(
                    mapOf(
                        "code" to "size",
                        "title" to "Размер",
                        "values" to listOf(mapOf("code" to "l", "title" to "L")),
                    )
                ),
                "variants" to listOf(
                    mapOf(
                        "sku" to "DUP-SKU-1",
                        "options" to listOf(mapOf("optionGroupCode" to "size", "optionValueCode" to "l")),
                    )
                ),
            )
        )

        assertTrue(errorBody.contains("already exists"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `variant with unknown option value returns validation error`() {
        val categoryId = createCategory(externalId = "cat-unknown-value", slug = "unknown-value")

        val request = mapOf(
            "categoryId" to categoryId,
            "title" to "Товар",
            "priceMinor" to 1000,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "color",
                    "title" to "Цвет",
                    "values" to listOf(mapOf("code" to "black", "title" to "Черный")),
                )
            ),
            "variants" to listOf(
                mapOf(
                    "sku" to "UNKNOWN-VALUE-1",
                    "options" to listOf(mapOf("optionGroupCode" to "color", "optionValueCode" to "white")),
                )
            ),
        )

        val errorBody = upsertProductAsAdminExpectBadRequest(request)
        assertTrue(errorBody.contains("unknown option value"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `duplicate product slug returns bad request with validation error`() {
        val categoryId = createCategory(externalId = "cat-dup-product-slug", slug = "dup-product-slug")

        upsertProductAsAdmin(
            mapOf(
                "categoryId" to categoryId,
                "title" to "Товар 1",
                "slug" to "same-product-slug",
                "priceMinor" to 1000,
                "unit" to "PIECE",
                "countStep" to 1,
                "isActive" to true,
            )
        )

        val errorBody = upsertProductAsAdminExpectBadRequest(
            mapOf(
                "categoryId" to categoryId,
                "title" to "Товар 2",
                "slug" to "same-product-slug",
                "priceMinor" to 1200,
                "unit" to "PIECE",
                "countStep" to 1,
                "isActive" to true,
            )
        )

        assertTrue(errorBody.contains("\"code\":\"VALIDATION_ERROR\""))
        assertTrue(errorBody.contains("unique constraint"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `variant with two values from same group returns validation error`() {
        val categoryId = createCategory(externalId = "cat-double-group", slug = "double-group")

        val request = mapOf(
            "categoryId" to categoryId,
            "title" to "Товар",
            "priceMinor" to 1000,
            "unit" to "PIECE",
            "countStep" to 1,
            "isActive" to true,
            "optionGroups" to listOf(
                mapOf(
                    "code" to "color",
                    "title" to "Цвет",
                    "values" to listOf(
                        mapOf("code" to "black", "title" to "Черный"),
                        mapOf("code" to "white", "title" to "Белый"),
                    ),
                )
            ),
            "variants" to listOf(
                mapOf(
                    "sku" to "DOUBLE-GROUP-1",
                    "options" to listOf(
                        mapOf("optionGroupCode" to "color", "optionValueCode" to "black"),
                        mapOf("optionGroupCode" to "color", "optionValueCode" to "white"),
                    ),
                )
            ),
        )

        val errorBody = upsertProductAsAdminExpectBadRequest(request)
        assertTrue(errorBody.contains("multiple values for option group"))
    }

    private fun createCategory(externalId: String, slug: String): UUID {
        val now = Instant.now()
        val category = categoryRepository.save(
            CatalogCategory(
                id = UUID.randomUUID(),
                externalId = externalId,
                name = slug,
                slug = slug,
                imageUrl = null,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )
        return category.id
    }

    private fun upsertProductAsAdmin(request: Any): JsonNode {
        val response = mockMvc.perform(
            post("/api/v1/admin/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(response)
    }

    private fun upsertProductAsAdminExpectBadRequest(request: Any): String {
        return mockMvc.perform(
            post("/api/v1/admin/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        ).andExpect(status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
    }

    private fun getProductDetails(productId: UUID): JsonNode {
        val response = mockMvc.perform(
            get("/api/v1/catalog/products/{productId}", productId)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(response)
    }

    private fun getAdminProductDetails(productId: UUID): JsonNode {
        val response = mockMvc.perform(
            get("/api/v1/admin/products/{productId}", productId)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(response)
    }
}
