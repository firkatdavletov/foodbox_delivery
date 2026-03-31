package ru.foodbox.delivery.modules.catalogimport.modifieroption.application.validation

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierOptionCommand
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.modifieroption.domain.model.ModifierOptionImportRow

@Component
class ModifierOptionImportRowValidator {

    fun validateDuplicates(rows: List<ModifierOptionImportRow>): List<CatalogImportRowError> {
        val errors = mutableListOf<CatalogImportRowError>()
        val firstRowByKey = mutableMapOf<String, Int>()

        rows.forEach { row ->
            firstRowByKey.putIfAbsent(row.rowKey, row.rowNumber)?.let { firstRowNumber ->
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.rowKey,
                    errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                    message = "Duplicate modifier option key '${row.rowKey}' in rows $firstRowNumber and ${row.rowNumber}",
                )
            }
        }

        return errors
    }

    fun validateBusinessRules(rows: List<ModifierOptionImportRow>): List<CatalogImportRowError> {
        return rows.mapNotNull(::validateBusinessRule)
    }

    fun validateGroupReferences(
        rows: List<ModifierOptionImportRow>,
        existingGroupCodes: Set<String>,
    ): List<CatalogImportRowError> {
        return rows.mapNotNull { row ->
            if (existingGroupCodes.contains(row.groupCode)) {
                return@mapNotNull null
            }

            CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.MODIFIER_GROUP_NOT_FOUND,
                message = "Modifier group '${row.groupCode}' not found",
            )
        }
    }

    fun validateMergedGroupState(
        group: ModifierGroup,
        affectedRows: List<ModifierOptionImportRow>,
        mergedOptions: List<UpsertModifierOptionCommand>,
    ): List<CatalogImportRowError> {
        val activeDefaultCount = mergedOptions.count { it.isActive && it.isDefault }
        if (activeDefaultCount <= group.maxSelected) {
            return emptyList()
        }

        return affectedRows.map { row ->
            CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.INVALID_MIN_MAX_RULE,
                message = "Group '${group.code}' would have $activeDefaultCount active default option(s), which exceeds max_selected=${group.maxSelected}",
            )
        }
    }

    private fun validateBusinessRule(row: ModifierOptionImportRow): CatalogImportRowError? {
        if (row.price < 0) {
            return CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field 'price' must be greater than or equal to zero",
            )
        }
        if (row.priceType == ModifierPriceType.FREE && row.price != 0L) {
            return CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field 'price' must be zero when price_type is FREE",
            )
        }
        return null
    }
}
