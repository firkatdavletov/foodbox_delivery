package ru.foodbox.delivery.modules.catalogimport.modifieroption.application

import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogModifierGroupService
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierGroupCommand
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierOptionCommand
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
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
import ru.foodbox.delivery.modules.catalogimport.modifieroption.application.mapping.ModifierOptionCsvRowMapper
import ru.foodbox.delivery.modules.catalogimport.modifieroption.application.validation.ModifierOptionImportRowValidator
import ru.foodbox.delivery.modules.catalogimport.modifieroption.domain.model.ModifierOptionImportRow

@Component
class ModifierOptionImportProcessor(
    private val modifierGroupRepository: ModifierGroupRepository,
    private val modifierOptionRepository: ModifierOptionRepository,
    private val catalogModifierGroupService: CatalogModifierGroupService,
    private val rowMapper: ModifierOptionCsvRowMapper,
    private val rowValidator: ModifierOptionImportRowValidator,
    private val reportBuilder: CatalogImportReportBuilder,
) : CatalogImportProcessor {

    override fun importType(): CatalogImportType = CatalogImportType.MODIFIER_OPTION

    override fun process(rows: List<CsvRow>, mode: CatalogImportMode): CatalogImportReport {
        val stats = ImportProcessingStats()
        val mappedRows = mutableListOf<ModifierOptionImportRow>()
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
        val groupsByCode = modifierGroupRepository.findAllByCodes(candidates.map { it.groupCode }.toSet())
            .associateBy { it.code }
        val groupErrors = rowValidator.validateGroupReferences(candidates, groupsByCode.keys)
        stats.rowErrors += groupErrors
        blockedRows += groupErrors.map { it.rowNumber }

        val rowsToProcess = candidates.filterNot { blockedRows.contains(it.rowNumber) }
        val existingOptionsByGroupId = modifierOptionRepository.findAllByGroupIds(groupsByCode.values.map { it.id })
            .groupBy { it.groupId }

        rowsToProcess.groupBy { it.groupCode }.toSortedMap().forEach { (groupCode, groupRows) ->
            val group = groupsByCode[groupCode] ?: return@forEach
            val existingOptions = existingOptionsByGroupId[group.id].orEmpty()
            val existingOptionsByCode = existingOptions.associateBy { it.code }
            val rowsSorted = groupRows.sortedBy { it.rowNumber }
            val rowsToApply = when (mode) {
                CatalogImportMode.CREATE_ONLY -> rowsSorted.filterNot { existingOptionsByCode.containsKey(it.optionCode) }
                else -> rowsSorted
            }
            val skippedCount = rowsSorted.size - rowsToApply.size
            val createdCount = rowsToApply.count { !existingOptionsByCode.containsKey(it.optionCode) }
            val updatedCount = rowsToApply.size - createdCount

            if (rowsToApply.isEmpty()) {
                stats.successCount += rowsSorted.size
                stats.skippedCount += skippedCount
                return@forEach
            }

            val mergedOptions = mergeOptions(existingOptions, rowsToApply)
            val mergedStateErrors = rowValidator.validateMergedGroupState(group, rowsToApply, mergedOptions)
            if (mergedStateErrors.isNotEmpty()) {
                stats.successCount += skippedCount
                stats.skippedCount += skippedCount
                stats.rowErrors += mergedStateErrors
                return@forEach
            }

            if (mode == CatalogImportMode.VALIDATE_ONLY) {
                stats.successCount += rowsSorted.size
                return@forEach
            }

            try {
                catalogModifierGroupService.upsert(
                    UpsertModifierGroupCommand(
                        id = group.id,
                        code = group.code,
                        name = group.name,
                        minSelected = group.minSelected,
                        maxSelected = group.maxSelected,
                        isRequired = group.isRequired,
                        isActive = group.isActive,
                        sortOrder = group.sortOrder,
                        options = mergedOptions,
                    )
                )
                stats.successCount += rowsSorted.size
                stats.skippedCount += skippedCount
                stats.createdCount += createdCount
                stats.updatedCount += updatedCount
            } catch (ex: IllegalArgumentException) {
                stats.successCount += skippedCount
                stats.skippedCount += skippedCount
                stats.rowErrors += groupedError(rowsToApply, CatalogImportErrorCode.INVALID_RELATION, ex.message ?: "Modifier option validation failed")
            } catch (ex: DataAccessException) {
                stats.successCount += skippedCount
                stats.skippedCount += skippedCount
                stats.rowErrors += groupedError(rowsToApply, CatalogImportErrorCode.PERSISTENCE_ERROR, ex.message ?: "Failed to persist modifier options")
            } catch (ex: RuntimeException) {
                stats.successCount += skippedCount
                stats.skippedCount += skippedCount
                stats.rowErrors += groupedError(rowsToApply, CatalogImportErrorCode.PERSISTENCE_ERROR, ex.message ?: "Failed to persist modifier options")
            }
        }

        return reportBuilder.build(
            importType = importType(),
            importMode = mode,
            totalRows = rows.size,
            stats = stats,
        )
    }

    private fun mergeOptions(
        existingOptions: List<ModifierOption>,
        rowsToApply: List<ModifierOptionImportRow>,
    ): List<UpsertModifierOptionCommand> {
        val merged = linkedMapOf<String, UpsertModifierOptionCommand>()
        existingOptions.forEach { option ->
            merged[option.code] = option.toCommand()
        }
        rowsToApply.forEach { row ->
            merged[row.optionCode] = row.toCommand()
        }
        return merged.values
            .sortedWith(compareBy<UpsertModifierOptionCommand> { it.sortOrder }.thenBy { it.name })
    }

    private fun ModifierOption.toCommand(): UpsertModifierOptionCommand {
        return UpsertModifierOptionCommand(
            code = code,
            name = name,
            description = description,
            priceType = priceType,
            price = price,
            applicationScope = applicationScope,
            isDefault = isDefault,
            isActive = isActive,
            sortOrder = sortOrder,
        )
    }

    private fun ModifierOptionImportRow.toCommand(): UpsertModifierOptionCommand {
        return UpsertModifierOptionCommand(
            code = optionCode,
            name = name,
            description = description,
            priceType = priceType,
            price = price,
            applicationScope = applicationScope,
            isDefault = isDefault,
            isActive = isActive,
            sortOrder = sortOrder,
        )
    }

    private fun groupedError(
        rows: List<ModifierOptionImportRow>,
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
