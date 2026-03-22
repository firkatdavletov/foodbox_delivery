package ru.foodbox.delivery.modules.catalogimport.application.processor

import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductOptionGroupCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductOptionValueCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantOptionCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductCommand
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalogimport.application.mapping.ProductCsvRowMapper
import ru.foodbox.delivery.modules.catalogimport.application.report.CatalogImportReportBuilder
import ru.foodbox.delivery.modules.catalogimport.application.report.ImportProcessingStats
import ru.foodbox.delivery.modules.catalogimport.application.validation.ProductImportRowValidator
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductImportRow

@Component
class ProductImportProcessor(
    private val categoryRepository: CatalogCategoryRepository,
    private val productRepository: CatalogProductRepository,
    private val catalogService: CatalogService,
    private val rowMapper: ProductCsvRowMapper,
    private val rowValidator: ProductImportRowValidator,
    private val reportBuilder: CatalogImportReportBuilder,
) : CatalogImportProcessor {

    override fun importType(): CatalogImportType = CatalogImportType.PRODUCT

    override fun process(rows: List<CsvRow>, mode: CatalogImportMode): CatalogImportReport {
        val stats = ImportProcessingStats()
        val mappedRows = mutableListOf<ProductImportRow>()
        rows.forEach { csvRow ->
            val mappingResult = rowMapper.map(csvRow)
            mappingResult.row?.let(mappedRows::add)
            stats.rowErrors += mappingResult.errors
        }

        val blockedRows = stats.rowErrors.map { it.rowNumber }.toMutableSet()

        val duplicateErrors = rowValidator.validateDuplicates(mappedRows)
        stats.rowErrors += duplicateErrors
        blockedRows += duplicateErrors.map { it.rowNumber }

        val candidates = mappedRows.filterNot { blockedRows.contains(it.rowNumber) }

        val categoriesByExternalId = categoryRepository
            .findAllByExternalIdIn(candidates.mapNotNull { it.categoryExternalId }.toSet())
            .mapNotNull { category -> category.externalId?.let { it to category } }
            .toMap()

        val categoryIdsToFind = candidates.mapNotNull { it.categoryId }.toSet()
        val categoriesById = categoryIdsToFind.mapNotNull { categoryId ->
            categoryRepository.findById(categoryId)?.let { categoryId to it }
        }.toMap()

        val categoryErrors = rowValidator.validateCategoryReferences(
            rows = candidates,
            existingCategoryExternalIds = categoriesByExternalId.keys,
            existingCategoryIds = categoriesById.keys,
        )
        stats.rowErrors += categoryErrors
        blockedRows += categoryErrors.map { it.rowNumber }

        val rowsToProcess = candidates.filterNot { blockedRows.contains(it.rowNumber) }
        val productGroups = rowsToProcess
            .groupBy(ProductImportRow::productKey)
            .values
            .sortedBy { group -> group.minOf { it.rowNumber } }

        val existingByExternalId = productRepository
            .findAllByExternalIdIn(productGroups.mapNotNull { group -> group.firstNotNullOfOrNull { it.productExternalId } }.toSet())
            .mapNotNull { product -> product.externalId?.let { it to product } }
            .toMap()
            .toMutableMap()

        val existingBySlug = productRepository
            .findAllBySlugIn(productGroups.map { it.first().productSlug }.toSet())
            .associateBy { it.slug }
            .toMutableMap()

        productGroups.forEach { groupRows ->
            val representative = groupRows.minByOrNull { it.rowNumber } ?: return@forEach
            val resolution = resolveExisting(groupRows, existingByExternalId, existingBySlug)
            if (resolution.error != null) {
                stats.rowErrors += resolution.error
                return@forEach
            }

            val existing = resolution.existing
            if (mode == CatalogImportMode.VALIDATE_ONLY) {
                stats.successCount += groupRows.size
                return@forEach
            }

            if (mode == CatalogImportMode.CREATE_ONLY && existing != null) {
                stats.successCount += groupRows.size
                stats.skippedCount += groupRows.size
                return@forEach
            }

            val aggregate = aggregateProduct(groupRows)
            val category = resolveCategory(
                categoryExternalId = aggregate.categoryExternalId,
                categoryId = aggregate.categoryId,
                categoriesByExternalId = categoriesByExternalId,
                categoriesById = categoriesById,
            )

            if (category == null) {
                stats.rowErrors += CatalogImportRowError(
                    rowNumber = representative.rowNumber,
                    rowKey = representative.rowKey,
                    errorCode = CatalogImportErrorCode.CATEGORY_NOT_FOUND,
                    message = "Category reference is not resolved for product '${representative.productKey}'",
                )
                return@forEach
            }

            val basePriceMinor = aggregate.productPriceMinor ?: aggregate.variants.firstNotNullOfOrNull { it.priceMinor }
            if (basePriceMinor == null) {
                stats.rowErrors += CatalogImportRowError(
                    rowNumber = representative.rowNumber,
                    rowKey = representative.rowKey,
                    errorCode = CatalogImportErrorCode.MISSING_REQUIRED_FIELD,
                    message = "Field 'product_price_minor' is required when product has no variant price fallback",
                )
                return@forEach
            }

            val command = UpsertProductCommand(
                id = existing?.id,
                externalId = aggregate.productExternalId,
                categoryId = category.id,
                title = aggregate.productTitle,
                slug = aggregate.productSlug,
                description = aggregate.productDescription,
                priceMinor = basePriceMinor,
                oldPriceMinor = aggregate.productOldPriceMinor,
                sku = if (aggregate.variants.isEmpty()) aggregate.productSku else null,
                imageIds = emptyList(),
                unit = aggregate.productUnit,
                countStep = aggregate.productCountStep,
                isActive = aggregate.productIsActive,
                brand = aggregate.productBrand,
                sortOrder = aggregate.productSortOrder,
                optionGroups = aggregate.optionGroups,
                variants = aggregate.variants,
            )

            try {
                val saved = catalogService.upsertProduct(command)
                stats.successCount += groupRows.size
                if (existing == null) {
                    stats.createdCount += groupRows.size
                } else {
                    stats.updatedCount += groupRows.size
                    existing.externalId?.let { previousExternalId ->
                        if (saved.externalId != previousExternalId) {
                            existingByExternalId.remove(previousExternalId)
                        }
                    }
                    if (saved.slug != existing.slug) {
                        existingBySlug.remove(existing.slug)
                    }
                }

                saved.externalId?.let { existingByExternalId[it] = saved }
                existingBySlug[saved.slug] = saved
            } catch (ex: DataAccessException) {
                stats.rowErrors += persistenceError(representative, ex)
            } catch (ex: RuntimeException) {
                stats.rowErrors += persistenceError(representative, ex)
            }
        }

        return reportBuilder.build(
            importType = importType(),
            importMode = mode,
            totalRows = rows.size,
            stats = stats,
        )
    }

    private fun resolveExisting(
        rows: List<ProductImportRow>,
        byExternalId: Map<String, CatalogProduct>,
        bySlug: Map<String, CatalogProduct>,
    ): ProductMatchResolution {
        val productExternalId = rows.firstNotNullOfOrNull { it.productExternalId }
        val productSlug = rows.first().productSlug

        val byExternal = productExternalId?.let(byExternalId::get)
        val bySlugKey = bySlug[productSlug]
        if (byExternal != null && bySlugKey != null && byExternal.id != bySlugKey.id) {
            return ProductMatchResolution(
                existing = null,
                error = CatalogImportRowError(
                    rowNumber = rows.minOf { it.rowNumber },
                    rowKey = rows.first().rowKey,
                    errorCode = CatalogImportErrorCode.AMBIGUOUS_MATCH,
                    message = "product_external_id '$productExternalId' and slug '$productSlug' point to different products",
                ),
            )
        }

        return ProductMatchResolution(existing = byExternal ?: bySlugKey)
    }

    private fun resolveCategory(
        categoryExternalId: String?,
        categoryId: java.util.UUID?,
        categoriesByExternalId: Map<String, CatalogCategory>,
        categoriesById: Map<java.util.UUID, CatalogCategory>,
    ): CatalogCategory? {
        if (categoryExternalId != null) {
            return categoriesByExternalId[categoryExternalId]
        }

        if (categoryId != null) {
            return categoriesById[categoryId]
        }

        return null
    }

    private fun aggregateProduct(rows: List<ProductImportRow>): AggregatedProduct {
        val orderedRows = rows.sortedBy { it.rowNumber }
        val firstRow = orderedRows.first()
        val hasVariants = orderedRows.any(ProductImportRow::hasVariantData)

        val optionGroupsByCode = linkedMapOf<String, MutableOptionGroup>()
        val variants = mutableListOf<ReplaceProductVariantCommand>()

        if (hasVariants) {
            orderedRows.forEach { row ->
                row.options.sortedBy { it.position }.forEach { option ->
                    val optionGroup = optionGroupsByCode.getOrPut(option.optionGroupCode) {
                        MutableOptionGroup(
                            code = option.optionGroupCode,
                            title = option.optionGroupTitle,
                            sortOrder = option.position,
                        )
                    }

                    optionGroup.values.putIfAbsent(
                        option.optionValueCode,
                        MutableOptionValue(
                            code = option.optionValueCode,
                            title = option.optionValueTitle,
                            sortOrder = optionGroup.values.size,
                        ),
                    )
                }

                variants += ReplaceProductVariantCommand(
                    externalId = row.variantExternalId,
                    sku = row.variantSku ?: "",
                    title = row.variantTitle,
                    priceMinor = row.variantPriceMinor ?: row.productPriceMinor,
                    oldPriceMinor = row.variantOldPriceMinor ?: row.productOldPriceMinor,
                    imageIds = emptyList(),
                    sortOrder = row.variantSortOrder,
                    isActive = row.variantIsActive,
                    options = row.options.map { option ->
                        ReplaceProductVariantOptionCommand(
                            optionGroupCode = option.optionGroupCode,
                            optionValueCode = option.optionValueCode,
                        )
                    },
                )
            }
        }

        val optionGroups = optionGroupsByCode.values.map { group ->
            ReplaceProductOptionGroupCommand(
                code = group.code,
                title = group.title,
                sortOrder = group.sortOrder,
                values = group.values.values.map { value ->
                    ReplaceProductOptionValueCommand(
                        code = value.code,
                        title = value.title,
                        sortOrder = value.sortOrder,
                    )
                },
            )
        }

        return AggregatedProduct(
            productExternalId = orderedRows.firstNotNullOfOrNull { it.productExternalId },
            productSlug = firstRow.productSlug,
            productTitle = firstRow.productTitle,
            categoryExternalId = orderedRows.firstNotNullOfOrNull { it.categoryExternalId },
            categoryId = orderedRows.firstNotNullOfOrNull { it.categoryId },
            productDescription = orderedRows.firstNotNullOfOrNull { it.productDescription },
            productBrand = orderedRows.firstNotNullOfOrNull { it.productBrand },
            productImageUrl = orderedRows.firstNotNullOfOrNull { it.productImageUrl },
            productPriceMinor = orderedRows.firstNotNullOfOrNull { it.productPriceMinor },
            productOldPriceMinor = orderedRows.firstNotNullOfOrNull { it.productOldPriceMinor },
            productSku = orderedRows.firstNotNullOfOrNull { it.productSku },
            productUnit = orderedRows.firstNotNullOfOrNull { it.productUnit } ?: ProductUnit.PIECE,
            productCountStep = orderedRows.firstOrNull()?.productCountStep ?: 1,
            productIsActive = orderedRows.firstOrNull()?.productIsActive ?: true,
            productSortOrder = orderedRows.firstOrNull()?.productSortOrder,
            optionGroups = optionGroups,
            variants = variants,
        )
    }

    private fun persistenceError(row: ProductImportRow, ex: Exception): CatalogImportRowError {
        return CatalogImportRowError(
            rowNumber = row.rowNumber,
            rowKey = row.rowKey,
            errorCode = CatalogImportErrorCode.PERSISTENCE_ERROR,
            message = ex.message ?: "Failed to persist product",
        )
    }

    private data class ProductMatchResolution(
        val existing: CatalogProduct?,
        val error: CatalogImportRowError? = null,
    )

    private data class AggregatedProduct(
        val productExternalId: String?,
        val productSlug: String,
        val productTitle: String,
        val categoryExternalId: String?,
        val categoryId: java.util.UUID?,
        val productDescription: String?,
        val productBrand: String?,
        val productImageUrl: String?,
        val productPriceMinor: Long?,
        val productOldPriceMinor: Long?,
        val productSku: String?,
        val productUnit: ProductUnit,
        val productCountStep: Int,
        val productIsActive: Boolean,
        val productSortOrder: Int?,
        val optionGroups: List<ReplaceProductOptionGroupCommand>,
        val variants: List<ReplaceProductVariantCommand>,
    )

    private data class MutableOptionGroup(
        val code: String,
        val title: String,
        val sortOrder: Int,
        val values: LinkedHashMap<String, MutableOptionValue> = linkedMapOf(),
    )

    private data class MutableOptionValue(
        val code: String,
        val title: String,
        val sortOrder: Int,
    )
}
