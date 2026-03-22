package ru.foodbox.delivery.modules.catalogimport.application.processor

import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalogimport.application.mapping.CategoryCsvRowMapper
import ru.foodbox.delivery.modules.catalogimport.application.report.CatalogImportReportBuilder
import ru.foodbox.delivery.modules.catalogimport.application.report.ImportProcessingStats
import ru.foodbox.delivery.modules.catalogimport.application.validation.CategoryImportRowValidator
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.domain.model.CategoryImportRow
import java.time.Instant
import java.util.UUID

@Component
class CategoryImportProcessor(
    private val categoryRepository: CatalogCategoryRepository,
    private val rowMapper: CategoryCsvRowMapper,
    private val rowValidator: CategoryImportRowValidator,
    private val reportBuilder: CatalogImportReportBuilder,
) : CatalogImportProcessor {

    override fun importType(): CatalogImportType = CatalogImportType.CATEGORY

    override fun process(rows: List<CsvRow>, mode: CatalogImportMode): CatalogImportReport {
        val stats = ImportProcessingStats()
        val mappedRows = mutableListOf<CategoryImportRow>()
        rows.forEach { csvRow ->
            val mappingResult = rowMapper.map(csvRow)
            mappingResult.row?.let(mappedRows::add)
            stats.rowErrors += mappingResult.errors
        }

        val blockedRows = stats.rowErrors.map { it.rowNumber }.toMutableSet()
        val duplicateErrors = rowValidator.validateDuplicates(mappedRows)
        stats.rowErrors += duplicateErrors
        blockedRows += duplicateErrors.map { it.rowNumber }

        val existingByExternalId = categoryRepository.findAllByExternalIdIn(mappedRows.map { it.externalId }.toSet())
            .mapNotNull { category -> category.externalId?.let { it to category } }
            .toMap()
            .toMutableMap()
        val existingBySlug = categoryRepository.findAllBySlugIn(mappedRows.map { it.slug }.toSet())
            .associateBy { it.slug }
            .toMutableMap()

        val mappedCandidates = mappedRows.filterNot { blockedRows.contains(it.rowNumber) }
        mappedCandidates.forEach { row ->
            val resolution = resolveExisting(row, existingByExternalId, existingBySlug)
            if (resolution.error != null) {
                stats.rowErrors += resolution.error
                blockedRows += row.rowNumber
            }
        }

        val validRows = mappedCandidates.filterNot { blockedRows.contains(it.rowNumber) }
        val parentErrors = rowValidator.validateParentReferences(validRows, existingByExternalId.keys)
        stats.rowErrors += parentErrors
        blockedRows += parentErrors.map { it.rowNumber }

        val rowsToProcess = validRows.filterNot { blockedRows.contains(it.rowNumber) }
        if (mode == CatalogImportMode.VALIDATE_ONLY) {
            stats.successCount = rowsToProcess.size
            return reportBuilder.build(
                importType = importType(),
                importMode = mode,
                totalRows = rows.size,
                stats = stats,
            )
        }

        persistRows(rowsToProcess, mode, stats, existingByExternalId, existingBySlug)

        return reportBuilder.build(
            importType = importType(),
            importMode = mode,
            totalRows = rows.size,
            stats = stats,
        )
    }

    private fun persistRows(
        rows: List<CategoryImportRow>,
        mode: CatalogImportMode,
        stats: ImportProcessingStats,
        knownByExternalId: MutableMap<String, CatalogCategory>,
        knownBySlug: MutableMap<String, CatalogCategory>,
    ) {
        val pendingRows = rows.toMutableList()
        while (pendingRows.isNotEmpty()) {
            var hasProgress = false
            val iterator = pendingRows.iterator()

            while (iterator.hasNext()) {
                val row = iterator.next()
                val parentId = row.parentExternalId?.let { knownByExternalId[it]?.id }
                if (row.parentExternalId != null && parentId == null) {
                    continue
                }

                val resolution = resolveExisting(row, knownByExternalId, knownBySlug)
                if (resolution.error != null) {
                    stats.rowErrors += resolution.error
                    iterator.remove()
                    hasProgress = true
                    continue
                }

                val existing = resolution.existing
                if (mode == CatalogImportMode.CREATE_ONLY && existing != null) {
                    stats.successCount += 1
                    stats.skippedCount += 1
                    iterator.remove()
                    hasProgress = true
                    continue
                }

                val now = Instant.now()
                val category = if (existing != null) {
                    existing.copy(
                        externalId = row.externalId,
                        name = row.name,
                        slug = row.slug,
                        parentId = parentId,
                        description = row.description,
                        sortOrder = row.sortOrder,
                        isActive = row.isActive,
                        updatedAt = now,
                    )
                } else {
                    CatalogCategory(
                        id = UUID.randomUUID(),
                        name = row.name,
                        slug = row.slug,
                        imageUrls = emptyList(),
                        isActive = row.isActive,
                        createdAt = now,
                        updatedAt = now,
                        externalId = row.externalId,
                        parentId = parentId,
                        description = row.description,
                        sortOrder = row.sortOrder,
                    )
                }

                try {
                    val saved = categoryRepository.save(category)
                    stats.successCount += 1
                    if (existing == null) {
                        stats.createdCount += 1
                    } else {
                        stats.updatedCount += 1
                        existing.externalId?.let { previousExternalId ->
                            if (saved.externalId != previousExternalId) {
                                knownByExternalId.remove(previousExternalId)
                            }
                        }
                        if (saved.slug != existing.slug) {
                            knownBySlug.remove(existing.slug)
                        }
                    }
                    saved.externalId?.let { knownByExternalId[it] = saved }
                    knownBySlug[saved.slug] = saved
                } catch (ex: DataAccessException) {
                    stats.rowErrors += persistenceError(row, ex)
                } catch (ex: RuntimeException) {
                    stats.rowErrors += persistenceError(row, ex)
                }

                iterator.remove()
                hasProgress = true
            }

            if (!hasProgress) {
                pendingRows.forEach { row ->
                    stats.rowErrors += CatalogImportRowError(
                        rowNumber = row.rowNumber,
                        rowKey = row.externalId,
                        errorCode = CatalogImportErrorCode.PARENT_CATEGORY_NOT_FOUND,
                        message = "Parent category '${row.parentExternalId}' cannot be resolved",
                    )
                }
                break
            }
        }
    }

    private fun resolveExisting(
        row: CategoryImportRow,
        byExternalId: Map<String, CatalogCategory>,
        bySlug: Map<String, CatalogCategory>,
    ): CategoryMatchResolution {
        val byExternal = byExternalId[row.externalId]
        val bySlugKey = bySlug[row.slug]
        if (byExternal != null && bySlugKey != null && byExternal.id != bySlugKey.id) {
            return CategoryMatchResolution(
                existing = null,
                error = CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.AMBIGUOUS_MATCH,
                    message = "external_id '${row.externalId}' and slug '${row.slug}' point to different categories",
                ),
            )
        }
        return CategoryMatchResolution(existing = byExternal ?: bySlugKey)
    }

    private fun persistenceError(row: CategoryImportRow, ex: Exception): CatalogImportRowError {
        return CatalogImportRowError(
            rowNumber = row.rowNumber,
            rowKey = row.externalId,
            errorCode = CatalogImportErrorCode.PERSISTENCE_ERROR,
            message = ex.message ?: "Failed to persist category",
        )
    }

    private data class CategoryMatchResolution(
        val existing: CatalogCategory?,
        val error: CatalogImportRowError? = null,
    )
}
