package ru.foodbox.delivery.modules.catalogimport.application.processor

import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalogimport.application.mapping.ProductCsvRowMapper
import ru.foodbox.delivery.modules.catalogimport.application.report.CatalogImportReportBuilder
import ru.foodbox.delivery.modules.catalogimport.application.report.ImportProcessingStats
import ru.foodbox.delivery.modules.catalogimport.application.support.SlugNormalizer
import ru.foodbox.delivery.modules.catalogimport.application.validation.ProductImportRowValidator
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductImportRow
import java.time.Instant
import java.util.UUID

@Component
class ProductImportProcessor(
    private val categoryRepository: CatalogCategoryRepository,
    private val productRepository: CatalogProductRepository,
    private val rowMapper: ProductCsvRowMapper,
    private val rowValidator: ProductImportRowValidator,
    private val slugNormalizer: SlugNormalizer,
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
        val categoriesByExternalId = categoryRepository.findAllByExternalIdIn(candidates.map { it.categoryExternalId }.toSet())
            .mapNotNull { category -> category.externalId?.let { it to category } }
            .toMap()
        val categoryErrors = rowValidator.validateCategoryReferences(candidates, categoriesByExternalId.keys)
        stats.rowErrors += categoryErrors
        blockedRows += categoryErrors.map { it.rowNumber }

        val rowsToProcess = candidates.filterNot { blockedRows.contains(it.rowNumber) }
        val existingByExternalId = productRepository.findAllByExternalIdIn(rowsToProcess.map { it.externalId }.toSet())
            .mapNotNull { product -> product.externalId?.let { it to product } }
            .toMap()
            .toMutableMap()
        val existingBySku = productRepository.findAllBySkuIn(rowsToProcess.map { it.sku }.toSet())
            .mapNotNull { product -> product.sku?.let { it to product } }
            .toMap()
            .toMutableMap()

        rowsToProcess.forEach { row ->
            val resolution = resolveExisting(row, existingByExternalId, existingBySku)
            if (resolution.error != null) {
                stats.rowErrors += resolution.error
                return@forEach
            }

            val existing = resolution.existing
            if (mode == CatalogImportMode.VALIDATE_ONLY) {
                stats.successCount += 1
                return@forEach
            }

            if (mode == CatalogImportMode.CREATE_ONLY && existing != null) {
                stats.successCount += 1
                stats.skippedCount += 1
                return@forEach
            }

            val categoryId = categoriesByExternalId[row.categoryExternalId]?.id
            if (categoryId == null) {
                stats.rowErrors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.CATEGORY_NOT_FOUND,
                    message = "Category with external_id '${row.categoryExternalId}' not found",
                )
                return@forEach
            }

            val now = Instant.now()
            val product = if (existing != null) {
                existing.copy(
                    externalId = row.externalId,
                    categoryId = categoryId,
                    title = row.name,
                    slug = slugNormalizer.normalize(row.slug, row.name),
                    description = row.description,
                    priceMinor = row.priceMinor,
                    oldPriceMinor = row.oldPriceMinor,
                    sku = row.sku,
                    brand = row.brand,
                    imageUrl = row.imageUrl,
                    sortOrder = row.sortOrder,
                    isActive = row.isActive,
                    updatedAt = now,
                )
            } else {
                CatalogProduct(
                    id = UUID.randomUUID(),
                    categoryId = categoryId,
                    title = row.name,
                    slug = slugNormalizer.normalize(row.slug, row.name),
                    description = row.description,
                    priceMinor = row.priceMinor,
                    oldPriceMinor = row.oldPriceMinor,
                    sku = row.sku,
                    imageUrl = row.imageUrl,
                    unit = ProductUnit.PIECE,
                    countStep = 1,
                    isActive = row.isActive,
                    createdAt = now,
                    updatedAt = now,
                    externalId = row.externalId,
                    brand = row.brand,
                    sortOrder = row.sortOrder,
                )
            }

            try {
                val saved = productRepository.save(product)
                stats.successCount += 1
                if (existing == null) {
                    stats.createdCount += 1
                } else {
                    stats.updatedCount += 1
                    existing.externalId?.let { previousExternalId ->
                        if (saved.externalId != previousExternalId) {
                            existingByExternalId.remove(previousExternalId)
                        }
                    }
                    existing.sku?.let { previousSku ->
                        if (saved.sku != previousSku) {
                            existingBySku.remove(previousSku)
                        }
                    }
                }
                saved.externalId?.let { existingByExternalId[it] = saved }
                saved.sku?.let { existingBySku[it] = saved }
            } catch (ex: DataAccessException) {
                stats.rowErrors += persistenceError(row, ex)
            } catch (ex: RuntimeException) {
                stats.rowErrors += persistenceError(row, ex)
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
        row: ProductImportRow,
        byExternalId: Map<String, CatalogProduct>,
        bySku: Map<String, CatalogProduct>,
    ): ProductMatchResolution {
        val byExternal = byExternalId[row.externalId]
        val bySkuKey = bySku[row.sku]
        if (byExternal != null && bySkuKey != null && byExternal.id != bySkuKey.id) {
            return ProductMatchResolution(
                existing = null,
                error = CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.AMBIGUOUS_MATCH,
                    message = "external_id '${row.externalId}' and sku '${row.sku}' point to different products",
                ),
            )
        }
        return ProductMatchResolution(existing = byExternal ?: bySkuKey)
    }

    private fun persistenceError(row: ProductImportRow, ex: Exception): CatalogImportRowError {
        return CatalogImportRowError(
            rowNumber = row.rowNumber,
            rowKey = row.externalId,
            errorCode = CatalogImportErrorCode.PERSISTENCE_ERROR,
            message = ex.message ?: "Failed to persist product",
        )
    }

    private data class ProductMatchResolution(
        val existing: CatalogProduct?,
        val error: CatalogImportRowError? = null,
    )
}
