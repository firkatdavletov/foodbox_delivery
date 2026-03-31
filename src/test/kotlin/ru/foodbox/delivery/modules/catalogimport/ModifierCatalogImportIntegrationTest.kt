package ru.foodbox.delivery.modules.catalogimport

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogProductModifiersService
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierOptionRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ProductModifierGroupRepository
import ru.foodbox.delivery.modules.catalogimport.application.CatalogImportService
import ru.foodbox.delivery.modules.catalogimport.application.command.ExecuteCatalogImportCommand
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
class ModifierCatalogImportIntegrationTest {

    @Autowired
    private lateinit var catalogImportService: CatalogImportService

    @Autowired
    private lateinit var categoryRepository: CatalogCategoryRepository

    @Autowired
    private lateinit var productRepository: CatalogProductRepository

    @Autowired
    private lateinit var modifierGroupRepository: ModifierGroupRepository

    @Autowired
    private lateinit var modifierOptionRepository: ModifierOptionRepository

    @Autowired
    private lateinit var productModifierGroupRepository: ProductModifierGroupRepository

    @Autowired
    private lateinit var catalogProductModifiersService: CatalogProductModifiersService

    @Test
    fun `imports modifier groups options and product links`() {
        val suffix = UUID.randomUUID().toString().substring(0, 8)
        val category = createCategory("cat-$suffix")
        val product = createProduct(category.id, "prd-$suffix")
        val groupCode = "gift_wrap_$suffix"

        val groupReport = executeImport(
            importType = CatalogImportType.MODIFIER_GROUP,
            importMode = CatalogImportMode.UPSERT,
            csv = """
                group_code,name,min_selected,max_selected,is_required,is_active,sort_order
                $groupCode,Gift wrap,0,1,false,true,10
            """.trimIndent(),
        )
        assertEquals(1, groupReport.successCount)
        assertEquals(1, groupReport.createdCount)
        assertEquals(0, groupReport.errorCount)

        val optionReport = executeImport(
            importType = CatalogImportType.MODIFIER_OPTION,
            importMode = CatalogImportMode.UPSERT,
            csv = """
                group_code,option_code,name,description,price_type,price,application_scope,is_default,is_active,sort_order
                $groupCode,classic,Classic wrap,Kraft paper,FIXED,1.50,PER_LINE,false,true,10
                $groupCode,card,Greeting card,Short printed message,FREE,0,PER_LINE,false,true,20
            """.trimIndent(),
        )
        assertEquals(2, optionReport.successCount)
        assertEquals(2, optionReport.createdCount)
        assertEquals(0, optionReport.errorCount)

        val linkReport = executeImport(
            importType = CatalogImportType.PRODUCT_MODIFIER_GROUP_LINK,
            importMode = CatalogImportMode.UPSERT,
            csv = """
                product_external_id,group_code,is_active,sort_order
                ${product.externalId},$groupCode,true,5
            """.trimIndent(),
        )
        assertEquals(1, linkReport.successCount)
        assertEquals(1, linkReport.createdCount)
        assertEquals(0, linkReport.errorCount)

        val group = modifierGroupRepository.findByCode(groupCode)
        assertNotNull(group)
        val options = modifierOptionRepository.findAllByGroupIds(listOf(group.id)).sortedBy { it.sortOrder }
        assertEquals(2, options.size)
        assertEquals("classic", options[0].code)
        assertEquals(150L, options[0].price)
        assertEquals("card", options[1].code)
        assertEquals(0L, options[1].price)

        val links = productModifierGroupRepository.findAllByProductId(product.id)
        assertEquals(1, links.size)
        assertEquals(group.id, links.single().modifierGroupId)

        val productModifiers = catalogProductModifiersService.getProductModifierGroups(product.id, activeOnly = false)
        assertEquals(1, productModifiers.size)
        assertEquals(groupCode, productModifiers.single().code)
        assertEquals(2, productModifiers.single().options.size)
    }

    @Test
    fun `validate only reports invalid min max and does not persist modifier groups`() {
        val suffix = UUID.randomUUID().toString().substring(0, 8)
        val validCode = "validate_ok_$suffix"
        val invalidCode = "validate_bad_$suffix"

        val report = executeImport(
            importType = CatalogImportType.MODIFIER_GROUP,
            importMode = CatalogImportMode.VALIDATE_ONLY,
            csv = """
                group_code,name,min_selected,max_selected,is_required,is_active,sort_order
                $validCode,Valid group,0,1,false,true,10
                $invalidCode,Invalid group,2,1,true,true,20
            """.trimIndent(),
        )

        assertEquals(1, report.successCount)
        assertEquals(0, report.createdCount)
        assertEquals(1, report.errorCount)
        assertEquals(CatalogImportErrorCode.INVALID_MIN_MAX_RULE, report.rowErrors.single().errorCode)
        assertNull(modifierGroupRepository.findByCode(validCode))
        assertNull(modifierGroupRepository.findByCode(invalidCode))
    }

    @Test
    fun `validate only reports missing references for product modifier links and does not persist them`() {
        val suffix = UUID.randomUUID().toString().substring(0, 8)
        val category = createCategory("cat-link-$suffix")
        val product = createProduct(category.id, "prd-link-$suffix")
        val groupCode = "addons_$suffix"

        executeImport(
            importType = CatalogImportType.MODIFIER_GROUP,
            importMode = CatalogImportMode.UPSERT,
            csv = """
                group_code,name,min_selected,max_selected,is_required,is_active,sort_order
                $groupCode,Add-ons,0,2,false,true,10
            """.trimIndent(),
        )

        val report = executeImport(
            importType = CatalogImportType.PRODUCT_MODIFIER_GROUP_LINK,
            importMode = CatalogImportMode.VALIDATE_ONLY,
            csv = """
                product_external_id,group_code,is_active,sort_order
                ${product.externalId},$groupCode,true,10
                missing-product-$suffix,$groupCode,true,20
                ${product.externalId},missing-group-$suffix,true,30
            """.trimIndent(),
        )

        assertEquals(1, report.successCount)
        assertEquals(2, report.errorCount)
        assertEquals(
            listOf(CatalogImportErrorCode.MODIFIER_GROUP_NOT_FOUND, CatalogImportErrorCode.PRODUCT_NOT_FOUND),
            report.rowErrors.map { it.errorCode }.sortedBy { it.name },
        )
        assertEquals(0, productModifierGroupRepository.findAllByProductId(product.id).size)
    }

    private fun executeImport(
        importType: CatalogImportType,
        importMode: CatalogImportMode,
        csv: String,
    ) = catalogImportService.execute(
        ExecuteCatalogImportCommand(
            importType = importType,
            importMode = importMode,
            csvBytes = csv.toByteArray(Charsets.UTF_8),
        )
    )

    private fun createCategory(externalId: String): CatalogCategory {
        val now = Instant.now()
        return categoryRepository.save(
            CatalogCategory(
                id = UUID.randomUUID(),
                name = "Category $externalId",
                slug = "category-$externalId",
                isActive = true,
                createdAt = now,
                updatedAt = now,
                externalId = externalId,
            )
        )
    }

    private fun createProduct(categoryId: UUID, externalId: String): CatalogProduct {
        val now = Instant.now()
        return productRepository.save(
            CatalogProduct(
                id = UUID.randomUUID(),
                categoryId = categoryId,
                title = "Product $externalId",
                slug = "product-$externalId",
                description = null,
                priceMinor = 4900,
                oldPriceMinor = null,
                sku = "SKU-$externalId",
                unit = ProductUnit.PIECE,
                countStep = 1,
                isActive = true,
                createdAt = now,
                updatedAt = now,
                externalId = externalId,
            )
        )
    }
}
