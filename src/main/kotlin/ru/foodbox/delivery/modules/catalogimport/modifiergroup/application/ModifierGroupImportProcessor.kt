package ru.foodbox.delivery.modules.catalogimport.modifiergroup.application

import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierOptionRepository
import ru.foodbox.delivery.modules.catalogimport.application.processor.CatalogImportProcessor
import ru.foodbox.delivery.modules.catalogimport.application.report.CatalogImportReportBuilder
import ru.foodbox.delivery.modules.catalogimport.application.report.ImportProcessingStats
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.modifiergroup.application.mapping.ModifierGroupCsvRowMapper
import ru.foodbox.delivery.modules.catalogimport.modifiergroup.application.validation.ModifierGroupImportRowValidator
import ru.foodbox.delivery.modules.catalogimport.modifiergroup.domain.model.ModifierGroupImportRow
import java.util.UUID

@Component
class ModifierGroupImportProcessor(
    private val modifierGroupRepository: ModifierGroupRepository,
    private val modifierOptionRepository: ModifierOptionRepository,
    private val rowMapper: ModifierGroupCsvRowMapper,
    private val rowValidator: ModifierGroupImportRowValidator,
    private val reportBuilder: CatalogImportReportBuilder,
) : CatalogImportProcessor {

    override fun importType(): CatalogImportType = CatalogImportType.MODIFIER_GROUP

    override fun process(rows: List<CsvRow>, mode: CatalogImportMode): CatalogImportReport {
        val stats = ImportProcessingStats()
        val mappedRows = mutableListOf<ModifierGroupImportRow>()
        rows.forEach { csvRow ->
            val mappingResult = rowMapper.map(csvRow)
            mappingResult.row?.let(mappedRows::add)
            stats.rowErrors += mappingResult.errors
        }

        val blockedRows = stats.rowErrors.map { it.rowNumber }.toMutableSet()
        val duplicateErrors = rowValidator.validateDuplicates(mappedRows)
        val businessRuleErrors = rowValidator.validateBusinessRules(mappedRows)
        stats.rowErrors += duplicateErrors
        stats.rowErrors += businessRuleErrors
        blockedRows += duplicateErrors.map { it.rowNumber }
        blockedRows += businessRuleErrors.map { it.rowNumber }

        val candidates = mappedRows.filterNot { blockedRows.contains(it.rowNumber) }
        val existingByCode = modifierGroupRepository.findAllByCodes(candidates.map { it.groupCode }.toSet())
            .associateBy { it.code }
            .toMutableMap()
        if (mode != CatalogImportMode.CREATE_ONLY) {
            val existingOptionsByGroupId = modifierOptionRepository.findAllByGroupIds(existingByCode.values.map { it.id })
                .groupBy { it.groupId }
            val groupStateErrors = rowValidator.validateExistingOptionState(candidates, existingByCode, existingOptionsByGroupId)
            stats.rowErrors += groupStateErrors
            blockedRows += groupStateErrors.map { it.rowNumber }
        }

        val rowsToProcess = candidates.filterNot { blockedRows.contains(it.rowNumber) }
        if (mode == CatalogImportMode.VALIDATE_ONLY) {
            stats.successCount = rowsToProcess.size
            return reportBuilder.build(
                importType = importType(),
                importMode = mode,
                totalRows = rows.size,
                stats = stats,
            )
        }

        rowsToProcess.forEach { row ->
            val existing = existingByCode[row.groupCode]
            if (mode == CatalogImportMode.CREATE_ONLY && existing != null) {
                stats.successCount += 1
                stats.skippedCount += 1
                return@forEach
            }

            try {
                val saved = modifierGroupRepository.save(
                    ModifierGroup(
                        id = existing?.id ?: UUID.randomUUID(),
                        code = row.groupCode,
                        name = row.name,
                        minSelected = row.minSelected,
                        maxSelected = row.maxSelected,
                        isRequired = row.isRequired,
                        isActive = row.isActive,
                        sortOrder = row.sortOrder,
                    )
                )
                existingByCode[saved.code] = saved
                stats.successCount += 1
                if (existing == null) {
                    stats.createdCount += 1
                } else {
                    stats.updatedCount += 1
                }
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

    private fun persistenceError(row: ModifierGroupImportRow, ex: Exception): CatalogImportRowError {
        return CatalogImportRowError(
            rowNumber = row.rowNumber,
            rowKey = row.rowKey,
            errorCode = CatalogImportErrorCode.PERSISTENCE_ERROR,
            message = ex.message ?: "Failed to persist modifier group",
        )
    }
}
