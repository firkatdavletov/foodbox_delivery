package ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.application

import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogProductModifiersService
import ru.foodbox.delivery.modules.catalog.modifier.application.command.ReplaceProductModifierGroupCommand
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ProductModifierGroupRepository
import ru.foodbox.delivery.modules.catalogimport.application.processor.CatalogImportProcessor
import ru.foodbox.delivery.modules.catalogimport.application.report.CatalogImportReportBuilder
import ru.foodbox.delivery.modules.catalogimport.application.report.ImportProcessingStats
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.application.mapping.ProductModifierGroupLinkCsvRowMapper
import ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.application.validation.ProductModifierGroupLinkImportRowValidator
import ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.domain.model.ProductModifierGroupLinkImportRow

@Component
class ProductModifierGroupLinkImportProcessor(
    private val productRepository: CatalogProductRepository,
    private val modifierGroupRepository: ModifierGroupRepository,
    private val productModifierGroupRepository: ProductModifierGroupRepository,
    private val catalogProductModifiersService: CatalogProductModifiersService,
    private val rowMapper: ProductModifierGroupLinkCsvRowMapper,
    private val rowValidator: ProductModifierGroupLinkImportRowValidator,
    private val reportBuilder: CatalogImportReportBuilder,
) : CatalogImportProcessor {

    override fun importType(): CatalogImportType = CatalogImportType.PRODUCT_MODIFIER_GROUP_LINK

    override fun process(rows: List<CsvRow>, mode: CatalogImportMode): CatalogImportReport {
        val stats = ImportProcessingStats()
        val mappedRows = mutableListOf<ProductModifierGroupLinkImportRow>()
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
        val productsByExternalId = productRepository.findAllByExternalIdIn(candidates.map { it.productExternalId }.toSet())
            .mapNotNull { product -> product.externalId?.let { it to product } }
            .toMap()
        val groupsByCode = modifierGroupRepository.findAllByCodes(candidates.map { it.groupCode }.toSet())
            .associateBy { it.code }

        val productErrors = rowValidator.validateProductReferences(candidates, productsByExternalId.keys)
        val groupErrors = rowValidator.validateGroupReferences(candidates, groupsByCode.keys)
        stats.rowErrors += productErrors
        stats.rowErrors += groupErrors
        blockedRows += productErrors.map { it.rowNumber }
        blockedRows += groupErrors.map { it.rowNumber }

        val rowsToProcess = candidates.filterNot { blockedRows.contains(it.rowNumber) }
        val existingLinksByProductId = productModifierGroupRepository.findAllByProductIds(productsByExternalId.values.map { it.id })
            .groupBy { it.productId }

        rowsToProcess.groupBy { it.productExternalId }.toSortedMap().forEach { (productExternalId, productRows) ->
            val product = productsByExternalId[productExternalId] ?: return@forEach
            val existingLinks = existingLinksByProductId[product.id].orEmpty()
            val existingLinksByGroupId = existingLinks.associateBy { it.modifierGroupId }
            val rowsSorted = productRows.sortedBy { it.rowNumber }
            val rowsToApply = when (mode) {
                CatalogImportMode.CREATE_ONLY -> rowsSorted.filterNot { row ->
                    val group = groupsByCode[row.groupCode] ?: return@filterNot false
                    existingLinksByGroupId.containsKey(group.id)
                }
                else -> rowsSorted
            }
            val skippedCount = rowsSorted.size - rowsToApply.size
            val createdCount = rowsToApply.count { row ->
                val group = groupsByCode.getValue(row.groupCode)
                !existingLinksByGroupId.containsKey(group.id)
            }
            val updatedCount = rowsToApply.size - createdCount

            if (rowsToApply.isEmpty()) {
                stats.successCount += rowsSorted.size
                stats.skippedCount += skippedCount
                return@forEach
            }

            val mergedLinks = mergeLinks(existingLinks, rowsToApply, groupsByCode)
            if (mode == CatalogImportMode.VALIDATE_ONLY) {
                stats.successCount += rowsSorted.size
                return@forEach
            }

            try {
                catalogProductModifiersService.replaceProductModifierGroups(product.id, mergedLinks)
                stats.successCount += rowsSorted.size
                stats.skippedCount += skippedCount
                stats.createdCount += createdCount
                stats.updatedCount += updatedCount
            } catch (ex: IllegalArgumentException) {
                stats.successCount += skippedCount
                stats.skippedCount += skippedCount
                stats.rowErrors += groupedError(rowsToApply, CatalogImportErrorCode.INVALID_RELATION, ex.message ?: "Product modifier link validation failed")
            } catch (ex: DataAccessException) {
                stats.successCount += skippedCount
                stats.skippedCount += skippedCount
                stats.rowErrors += groupedError(rowsToApply, CatalogImportErrorCode.PERSISTENCE_ERROR, ex.message ?: "Failed to persist product modifier links")
            } catch (ex: RuntimeException) {
                stats.successCount += skippedCount
                stats.skippedCount += skippedCount
                stats.rowErrors += groupedError(rowsToApply, CatalogImportErrorCode.PERSISTENCE_ERROR, ex.message ?: "Failed to persist product modifier links")
            }
        }

        return reportBuilder.build(
            importType = importType(),
            importMode = mode,
            totalRows = rows.size,
            stats = stats,
        )
    }

    private fun mergeLinks(
        existingLinks: List<ProductModifierGroup>,
        rowsToApply: List<ProductModifierGroupLinkImportRow>,
        groupsByCode: Map<String, ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup>,
    ): List<ReplaceProductModifierGroupCommand> {
        val merged = linkedMapOf<java.util.UUID, ReplaceProductModifierGroupCommand>()
        existingLinks.forEach { link ->
            merged[link.modifierGroupId] = ReplaceProductModifierGroupCommand(
                modifierGroupId = link.modifierGroupId,
                sortOrder = link.sortOrder,
                isActive = link.isActive,
            )
        }
        rowsToApply.forEach { row ->
            val group = groupsByCode.getValue(row.groupCode)
            merged[group.id] = ReplaceProductModifierGroupCommand(
                modifierGroupId = group.id,
                sortOrder = row.sortOrder,
                isActive = row.isActive,
            )
        }
        return merged.values
            .sortedWith(compareBy<ReplaceProductModifierGroupCommand> { it.sortOrder }.thenBy { it.modifierGroupId.toString() })
    }

    private fun groupedError(
        rows: List<ProductModifierGroupLinkImportRow>,
        errorCode: CatalogImportErrorCode,
        message: String,
    ): List<CatalogImportRowError> {
        return rows.map { row ->
            CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = errorCode,
                message = message,
            )
        }
    }
}
