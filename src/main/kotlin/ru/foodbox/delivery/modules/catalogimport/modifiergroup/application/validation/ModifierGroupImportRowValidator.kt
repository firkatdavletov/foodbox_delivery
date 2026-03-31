package ru.foodbox.delivery.modules.catalogimport.modifiergroup.application.validation

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.modifiergroup.domain.model.ModifierGroupImportRow

@Component
class ModifierGroupImportRowValidator {

    fun validateDuplicates(rows: List<ModifierGroupImportRow>): List<CatalogImportRowError> {
        val errors = mutableListOf<CatalogImportRowError>()
        val firstRowByCode = mutableMapOf<String, Int>()

        rows.forEach { row ->
            firstRowByCode.putIfAbsent(row.groupCode, row.rowNumber)?.let { firstRowNumber ->
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.rowKey,
                    errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                    message = "Duplicate group_code '${row.groupCode}' in rows $firstRowNumber and ${row.rowNumber}",
                )
            }
        }

        return errors
    }

    fun validateBusinessRules(rows: List<ModifierGroupImportRow>): List<CatalogImportRowError> {
        return rows.mapNotNull(::validateBusinessRule)
    }

    fun validateExistingOptionState(
        rows: List<ModifierGroupImportRow>,
        existingGroupsByCode: Map<String, ModifierGroup>,
        existingOptionsByGroupId: Map<java.util.UUID, List<ModifierOption>>,
    ): List<CatalogImportRowError> {
        return rows.mapNotNull { row ->
            val existingGroup = existingGroupsByCode[row.groupCode] ?: return@mapNotNull null
            val activeDefaultCount = existingOptionsByGroupId[existingGroup.id].orEmpty()
                .count { it.isActive && it.isDefault }
            if (activeDefaultCount <= row.maxSelected) {
                return@mapNotNull null
            }

            CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.INVALID_MIN_MAX_RULE,
                message = "Group '${row.groupCode}' has $activeDefaultCount active default option(s), which exceeds max_selected=${row.maxSelected}",
            )
        }
    }

    private fun validateBusinessRule(row: ModifierGroupImportRow): CatalogImportRowError? {
        if (row.minSelected < 0) {
            return CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field 'min_selected' must be greater than or equal to zero",
            )
        }
        if (row.maxSelected <= 0) {
            return CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.INVALID_MIN_MAX_RULE,
                message = "Field 'max_selected' must be greater than zero",
            )
        }
        if (row.maxSelected < row.minSelected) {
            return CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.INVALID_MIN_MAX_RULE,
                message = "Field 'max_selected' must be greater than or equal to 'min_selected'",
            )
        }
        if (row.isRequired && row.minSelected == 0) {
            return CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.INVALID_MIN_MAX_RULE,
                message = "Required group must have min_selected greater than zero",
            )
        }
        return null
    }
}
