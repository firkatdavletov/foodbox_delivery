package ru.foodbox.delivery.modules.catalog

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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogModifierGroupService
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierGroupCommand
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierOptionCommand
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa.ModifierGroupJpaRepository
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa.ModifierOptionJpaRepository
import ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.jpa.ProductModifierGroupJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogCategoryImageJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogCategoryJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductImageJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductOptionGroupJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductOptionValueJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantImageJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductVariantOptionValueJpaRepository
import ru.foodbox.delivery.modules.catalogimport.application.CatalogImportService
import ru.foodbox.delivery.modules.catalogimport.application.command.ExecuteCatalogImportCommand
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import ru.foodbox.delivery.modules.media.domain.MediaImageStatus
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import ru.foodbox.delivery.modules.media.domain.storage.ObjectStoragePort
import ru.foodbox.delivery.modules.media.infrastructure.persistence.entity.MediaImageEntity
import ru.foodbox.delivery.modules.media.infrastructure.persistence.jpa.MediaImageJpaRepository
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
    private lateinit var catalogModifierGroupService: CatalogModifierGroupService

    @Autowired
    private lateinit var productModifierGroupJpaRepository: ProductModifierGroupJpaRepository

    @Autowired
    private lateinit var modifierOptionJpaRepository: ModifierOptionJpaRepository

    @Autowired
    private lateinit var modifierGroupJpaRepository: ModifierGroupJpaRepository

    @Autowired
    private lateinit var variantOptionValueJpaRepository: CatalogProductVariantOptionValueJpaRepository

    @Autowired
    private lateinit var optionValueJpaRepository: CatalogProductOptionValueJpaRepository

    @Autowired
    private lateinit var variantJpaRepository: CatalogProductVariantJpaRepository

    @Autowired
    private lateinit var optionGroupJpaRepository: CatalogProductOptionGroupJpaRepository

    @Autowired
    private lateinit var variantImageJpaRepository: CatalogProductVariantImageJpaRepository

    @Autowired
    private lateinit var productImageJpaRepository: CatalogProductImageJpaRepository

    @Autowired
    private lateinit var categoryImageJpaRepository: CatalogCategoryImageJpaRepository

    @Autowired
    private lateinit var productJpaRepository: CatalogProductJpaRepository

    @Autowired
    private lateinit var categoryJpaRepository: CatalogCategoryJpaRepository

    @Autowired
    private lateinit var mediaImageJpaRepository: MediaImageJpaRepository

    @MockBean
    private lateinit var storagePort: ObjectStoragePort

    @BeforeEach
    fun cleanup() {
        Mockito.reset(storagePort)
        Mockito.doNothing().`when`(storagePort).moveObject(Mockito.anyString(), Mockito.anyString())
        Mockito.doAnswer { invocation ->
            "https://cdn.example.com/${invocation.getArgument<String>(0)}"
        }.`when`(storagePort).buildPublicUrl(Mockito.anyString())
        variantImageJpaRepository.deleteAllInBatch()
        productImageJpaRepository.deleteAllInBatch()
        categoryImageJpaRepository.deleteAllInBatch()
        mediaImageJpaRepository.deleteAllInBatch()
        productModifierGroupJpaRepository.deleteAllInBatch()
        modifierOptionJpaRepository.deleteAllInBatch()
        modifierGroupJpaRepository.deleteAllInBatch()
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
        val productImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/shirt-main.jpg",
            targetType = MediaTargetType.PRODUCT,
        )
        val blackVariantImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/shirt-black-s.jpg",
            targetType = MediaTargetType.VARIANT,
        )
        val whiteVariantImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/shirt-white-m.jpg",
            targetType = MediaTargetType.VARIANT,
        )

        val request = mapOf(
            "categoryId" to categoryId,
            "title" to "Футболка",
            "priceMinor" to 3000,
            "oldPriceMinor" to 3500,
            "imageIds" to listOf(productImageId),
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
                    "imageIds" to listOf(blackVariantImageId),
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
                    "imageIds" to listOf(whiteVariantImageId),
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
        assertEquals(true, upsertResponse["isConfigured"].asBoolean())
        val productId = UUID.fromString(upsertResponse.get("id").asText())
        val productImage = mediaImageJpaRepository.findById(productImageId).orElseThrow()
        assertEquals(productId, productImage.targetId)
        assertTrue(productImage.objectKey.startsWith("products/$productId/"))
        assertEquals("https://cdn.example.com/${productImage.objectKey}", upsertResponse["imageUrls"][0].asText())

        val details = getProductDetails(productId)
        assertEquals(true, details["isConfigured"].asBoolean())
        assertEquals("https://cdn.example.com/${productImage.objectKey}", details["imageUrls"][0].asText())
        assertEquals(2, details.get("optionGroups").size())
        assertEquals(2, details.get("variants").size())
        assertNotNull(details.get("defaultVariantId")?.asText())
        assertEquals("TSHIRT-BLACK-S", details.get("variants")[0].get("sku").asText())
        val blackVariantId = UUID.fromString(details["variants"][0]["id"].asText())
        val blackVariantImage = mediaImageJpaRepository.findById(blackVariantImageId).orElseThrow()
        assertEquals(blackVariantId, blackVariantImage.targetId)
        assertTrue(blackVariantImage.objectKey.startsWith("variants/$blackVariantId/"))
        assertEquals(
            "https://cdn.example.com/${blackVariantImage.objectKey}",
            details["variants"][0]["imageUrls"][0].asText(),
        )
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
        assertEquals(false, created["isConfigured"].asBoolean())
        val productId = UUID.fromString(created.get("id").asText())

        val details = getProductDetails(productId)
        assertEquals(false, details["isConfigured"].asBoolean())
        assertEquals(0, details.get("optionGroups").size())
        assertEquals(0, details.get("variants").size())
        assertEquals(0, details.get("imageUrls").size())
        assertTrue(details.get("defaultVariantId").isNull)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `product with modifier groups is configured in list and details`() {
        val categoryId = createCategory(externalId = "cat-modifiers", slug = "modifiers")
        val modifierGroup = catalogModifierGroupService.upsert(
            UpsertModifierGroupCommand(
                id = null,
                code = "extras",
                name = "Допы",
                minSelected = 0,
                maxSelected = 2,
                isRequired = false,
                isActive = true,
                sortOrder = 0,
                options = listOf(
                    UpsertModifierOptionCommand(
                        code = "gift-wrap",
                        name = "Подарочная упаковка",
                        description = null,
                        priceType = ModifierPriceType.FIXED,
                        price = 200,
                        applicationScope = ModifierApplicationScope.PER_LINE,
                        isDefault = false,
                        isActive = true,
                        sortOrder = 0,
                    )
                ),
            )
        )

        val created = upsertProductAsAdmin(
            mapOf(
                "categoryId" to categoryId,
                "title" to "Товар с модификаторами",
                "priceMinor" to 1800,
                "sku" to "MOD-ONLY-1",
                "unit" to "PIECE",
                "countStep" to 1,
                "isActive" to true,
                "modifierGroups" to listOf(
                    mapOf(
                        "modifierGroupId" to modifierGroup.group.id,
                        "sortOrder" to 0,
                        "isActive" to true,
                    )
                ),
            )
        )
        assertEquals(true, created["isConfigured"].asBoolean())

        val productId = UUID.fromString(created["id"].asText())
        val details = getProductDetails(productId)
        assertEquals(true, details["isConfigured"].asBoolean())
        assertEquals(1, details["modifierGroups"].size())

        val products = getProducts(categoryId)
        val listItem = products.first { it["id"].asText() == productId.toString() }
        assertEquals(true, listItem["isConfigured"].asBoolean())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `update product syncs image attachments and marks removed images as deleted`() {
        val categoryId = createCategory(externalId = "cat-image-sync", slug = "image-sync")
        val initialProductImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/image-sync-initial.jpg",
            targetType = MediaTargetType.PRODUCT,
        )
        val nextProductImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/image-sync-next.jpg",
            targetType = MediaTargetType.PRODUCT,
        )
        val initialVariantImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/image-sync-variant-initial.jpg",
            targetType = MediaTargetType.VARIANT,
        )
        val nextVariantImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/image-sync-variant-next.jpg",
            targetType = MediaTargetType.VARIANT,
        )

        val created = upsertProductAsAdmin(
            mapOf(
                "categoryId" to categoryId,
                "title" to "Худи",
                "priceMinor" to 4200,
                "imageIds" to listOf(initialProductImageId),
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
                        "sku" to "HOODIE-M",
                        "imageIds" to listOf(initialVariantImageId),
                        "options" to listOf(mapOf("optionGroupCode" to "size", "optionValueCode" to "m")),
                    )
                ),
            )
        )
        val productId = UUID.fromString(created["id"].asText())

        upsertProductAsAdmin(
            mapOf(
                "id" to productId,
                "categoryId" to categoryId,
                "title" to "Худи",
                "priceMinor" to 4300,
                "imageIds" to listOf(nextProductImageId),
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
                        "sku" to "HOODIE-M",
                        "imageIds" to listOf(nextVariantImageId),
                        "options" to listOf(mapOf("optionGroupCode" to "size", "optionValueCode" to "m")),
                    )
                ),
            )
        )

        val details = getProductDetails(productId)
        val nextProductImage = mediaImageJpaRepository.findById(nextProductImageId).orElseThrow()
        assertEquals(productId, nextProductImage.targetId)
        assertTrue(nextProductImage.objectKey.startsWith("products/$productId/"))
        assertEquals("https://cdn.example.com/${nextProductImage.objectKey}", details["imageUrls"][0].asText())
        val nextVariantId = UUID.fromString(details["variants"][0]["id"].asText())
        val nextVariantImage = mediaImageJpaRepository.findById(nextVariantImageId).orElseThrow()
        assertEquals(nextVariantId, nextVariantImage.targetId)
        assertTrue(nextVariantImage.objectKey.startsWith("variants/$nextVariantId/"))
        assertEquals(
            "https://cdn.example.com/${nextVariantImage.objectKey}",
            details["variants"][0]["imageUrls"][0].asText(),
        )
        assertEquals(
            MediaImageStatus.DELETED,
            mediaImageJpaRepository.findById(initialProductImageId).orElseThrow().status,
        )
        assertEquals(
            MediaImageStatus.DELETED,
            mediaImageJpaRepository.findById(initialVariantImageId).orElseThrow().status,
        )
        assertEquals(MediaImageStatus.READY, mediaImageJpaRepository.findById(nextProductImageId).orElseThrow().status)
        assertEquals(MediaImageStatus.READY, mediaImageJpaRepository.findById(nextVariantImageId).orElseThrow().status)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `attaching variant image keeps product image attached`() {
        val categoryId = createCategory(externalId = "cat-variant-image-keep", slug = "variant-image-keep")
        val productImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/variant-image-keep-product.jpg",
            targetType = MediaTargetType.PRODUCT,
        )
        val variantImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/variant-image-keep-variant.jpg",
            targetType = MediaTargetType.VARIANT,
        )

        val created = upsertProductAsAdmin(
            mapOf(
                "categoryId" to categoryId,
                "title" to "Куртка",
                "priceMinor" to 6800,
                "imageIds" to listOf(productImageId),
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
                        "sku" to "JACKET-M",
                        "options" to listOf(mapOf("optionGroupCode" to "size", "optionValueCode" to "m")),
                    )
                ),
            )
        )
        val productId = UUID.fromString(created["id"].asText())

        upsertProductAsAdmin(
            mapOf(
                "id" to productId,
                "categoryId" to categoryId,
                "title" to "Куртка",
                "priceMinor" to 6800,
                "imageIds" to listOf(productImageId),
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
                        "sku" to "JACKET-M",
                        "imageIds" to listOf(variantImageId),
                        "options" to listOf(mapOf("optionGroupCode" to "size", "optionValueCode" to "m")),
                    )
                ),
            )
        )

        val publicDetails = getProductDetails(productId)
        assertTrue(publicDetails.path("imageIds").isMissingNode)
        assertTrue(publicDetails.path("variants").path(0).path("imageIds").isMissingNode)

        val adminDetails = getAdminProductDetails(productId)
        val productImage = mediaImageJpaRepository.findById(productImageId).orElseThrow()
        assertEquals(MediaImageStatus.READY, productImage.status)
        assertEquals(productId, productImage.targetId)
        assertTrue(productImage.objectKey.startsWith("products/$productId/"))
        assertEquals(productImageId.toString(), adminDetails["imageIds"][0].asText())
        assertEquals("https://cdn.example.com/${productImage.objectKey}", publicDetails["imageUrls"][0].asText())

        val variantId = UUID.fromString(publicDetails["variants"][0]["id"].asText())
        val variantImage = mediaImageJpaRepository.findById(variantImageId).orElseThrow()
        assertEquals(MediaImageStatus.READY, variantImage.status)
        assertEquals(variantId, variantImage.targetId)
        assertTrue(variantImage.objectKey.startsWith("variants/$variantId/"))
        assertEquals(variantImageId.toString(), adminDetails["variants"][0]["imageIds"][0].asText())
        assertEquals(
            "https://cdn.example.com/${variantImage.objectKey}",
            publicDetails["variants"][0]["imageUrls"][0].asText(),
        )
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `upsert category syncs image attachments`() {
        val initialImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/categories/dresses-initial.jpg",
            targetType = MediaTargetType.CATEGORY,
        )
        val nextImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/categories/dresses-next.jpg",
            targetType = MediaTargetType.CATEGORY,
        )

        val created = upsertCategoryAsAdmin(
            mapOf(
                "name" to "Платья",
                "slug" to "dresses",
                "imageIds" to listOf(initialImageId),
                "isActive" to true,
            )
        )
        val categoryId = UUID.fromString(created["id"].asText())
        val initialCategoryImage = mediaImageJpaRepository.findById(initialImageId).orElseThrow()
        assertEquals(categoryId, initialCategoryImage.targetId)
        assertTrue(initialCategoryImage.objectKey.startsWith("categories/$categoryId/"))
        assertEquals("https://cdn.example.com/${initialCategoryImage.objectKey}", created["imageUrls"][0].asText())

        val updated = upsertCategoryAsAdmin(
            mapOf(
                "id" to categoryId,
                "name" to "Платья",
                "slug" to "dresses",
                "imageIds" to listOf(nextImageId),
                "isActive" to true,
            )
        )

        val nextCategoryImage = mediaImageJpaRepository.findById(nextImageId).orElseThrow()
        assertEquals(categoryId, nextCategoryImage.targetId)
        assertTrue(nextCategoryImage.objectKey.startsWith("categories/$categoryId/"))
        assertEquals("https://cdn.example.com/${nextCategoryImage.objectKey}", updated["imageUrls"][0].asText())
        assertEquals(MediaImageStatus.DELETED, mediaImageJpaRepository.findById(initialImageId).orElseThrow().status)
        assertEquals(MediaImageStatus.READY, mediaImageJpaRepository.findById(nextImageId).orElseThrow().status)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `delete category image removes attachment and marks media image deleted`() {
        val firstImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/categories/sale-first.jpg",
            targetType = MediaTargetType.CATEGORY,
        )
        val secondImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/categories/sale-second.jpg",
            targetType = MediaTargetType.CATEGORY,
        )

        val created = upsertCategoryAsAdmin(
            mapOf(
                "name" to "Распродажа",
                "slug" to "sale",
                "imageIds" to listOf(firstImageId, secondImageId),
                "isActive" to true,
            )
        )
        val categoryId = UUID.fromString(created["id"].asText())

        deleteCategoryImageAsAdmin(categoryId, firstImageId)

        val categories = getAdminCategories(isActive = true)
        val updatedCategory = categories.first { it["id"].asText() == categoryId.toString() }
        val secondImage = mediaImageJpaRepository.findById(secondImageId).orElseThrow()
        assertEquals(1, updatedCategory["imageUrls"].size())
        assertEquals("https://cdn.example.com/${secondImage.objectKey}", updatedCategory["imageUrls"][0].asText())
        assertEquals(MediaImageStatus.DELETED, mediaImageJpaRepository.findById(firstImageId).orElseThrow().status)
        assertEquals(MediaImageStatus.READY, secondImage.status)
        assertEquals(categoryId, secondImage.targetId)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `delete product image removes attachment and marks media image deleted`() {
        val categoryId = createCategory(externalId = "cat-delete-product-image", slug = "delete-product-image")
        val firstImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/delete-first.jpg",
            targetType = MediaTargetType.PRODUCT,
        )
        val secondImageId = createReadyImage(
            publicUrl = "https://cdn.example.com/products/delete-second.jpg",
            targetType = MediaTargetType.PRODUCT,
        )

        val created = upsertProductAsAdmin(
            mapOf(
                "categoryId" to categoryId,
                "title" to "Толстовка",
                "priceMinor" to 3900,
                "imageIds" to listOf(firstImageId, secondImageId),
                "unit" to "PIECE",
                "countStep" to 1,
                "isActive" to true,
                "optionGroups" to emptyList<Any>(),
                "variants" to emptyList<Any>(),
            )
        )
        val productId = UUID.fromString(created["id"].asText())

        deleteProductImageAsAdmin(productId, firstImageId)

        val details = getProductDetails(productId)
        val adminDetails = getAdminProductDetails(productId)
        val secondImage = mediaImageJpaRepository.findById(secondImageId).orElseThrow()
        assertEquals(1, details["imageUrls"].size())
        assertEquals("https://cdn.example.com/${secondImage.objectKey}", details["imageUrls"][0].asText())
        assertEquals(1, adminDetails["imageIds"].size())
        assertEquals(secondImageId.toString(), adminDetails["imageIds"][0].asText())
        assertEquals(MediaImageStatus.DELETED, mediaImageJpaRepository.findById(firstImageId).orElseThrow().status)
        assertEquals(MediaImageStatus.READY, secondImage.status)
        assertEquals(productId, secondImage.targetId)
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
        assertEquals(false, adminDetails.get("isConfigured").asBoolean())
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
    fun `import category with russian headers`() {
        val csv = """
            Внешний ID категории в каталоге (обязательное),Название категории (обязательное),Слаг категории (обязательное),Внешний ID родительской категории (необязательное),Описание категории (необязательное),Категория активна (необязательное),Порядок сортировки категории (необязательное)
            cat-root,Каталог,catalog,,Корневая категория,true,10
            cat-fruits,Фрукты,fruits,cat-root,Раздел фруктов,true,20
        """.trimIndent()

        val report = catalogImportService.execute(
            ExecuteCatalogImportCommand(
                importType = CatalogImportType.CATEGORY,
                importMode = CatalogImportMode.CREATE_ONLY,
                csvBytes = csv.toByteArray(),
            )
        )

        assertEquals(0, report.errorCount)
        assertEquals(2, report.successCount)
        assertNotNull(categoryRepository.findByExternalId("cat-root"))
        assertNotNull(categoryRepository.findByExternalId("cat-fruits"))
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
                imageUrls = emptyList(),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )
        return category.id
    }

    private fun createReadyImage(
        publicUrl: String,
        targetType: MediaTargetType,
        targetId: UUID? = null,
    ): UUID {
        val now = Instant.now()
        val keyPrefix = when (targetType) {
            MediaTargetType.PRODUCT -> "products"
            MediaTargetType.CATEGORY -> "categories"
            MediaTargetType.VARIANT -> "variants"
        }
        val image = MediaImageEntity(
            id = UUID.randomUUID(),
            targetType = targetType,
            targetId = targetId,
            bucket = "test-bucket",
            objectKey = "$keyPrefix/${targetId?.toString() ?: "unassigned"}/${UUID.randomUUID()}.jpg",
            originalFilename = "test.jpg",
            contentType = "image/jpeg",
            fileSize = 1024,
            status = MediaImageStatus.READY,
            publicUrl = publicUrl,
            createdAt = now,
            updatedAt = now,
        )
        return mediaImageJpaRepository.save(image).id
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

    private fun upsertCategoryAsAdmin(request: Any): JsonNode {
        val response = mockMvc.perform(
            post("/api/v1/admin/catalog/categories")
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

    private fun getProducts(categoryId: UUID? = null): JsonNode {
        val request = get("/api/v1/catalog/products")
            .accept(MediaType.APPLICATION_JSON)
        if (categoryId != null) {
            request.param("categoryId", categoryId.toString())
        }

        val response = mockMvc.perform(request)
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(response)
    }

    private fun getAdminCategories(isActive: Boolean): JsonNode {
        val response = mockMvc.perform(
            get("/api/v1/admin/catalog/categories")
                .param("isActive", isActive.toString())
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

    private fun deleteCategoryImageAsAdmin(categoryId: UUID, imageId: UUID) {
        mockMvc.perform(
            delete("/api/v1/admin/catalog/categories/{categoryId}/images/{imageId}", categoryId, imageId)
        ).andExpect(status().isNoContent)
    }

    private fun deleteProductImageAsAdmin(productId: UUID, imageId: UUID) {
        mockMvc.perform(
            delete("/api/v1/admin/catalog/products/{productId}/images/{imageId}", productId, imageId)
        ).andExpect(status().isNoContent)
    }
}
