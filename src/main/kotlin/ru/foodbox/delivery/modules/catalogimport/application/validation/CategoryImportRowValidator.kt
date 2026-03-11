package ru.foodbox.delivery.modules.catalogimport.application.validation

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.model.CategoryImportRow

@Component
class CategoryImportRowValidator {

    fun validateDuplicates(rows: List<CategoryImportRow>): List<CatalogImportRowError> {
        val errors = mutableListOf<CatalogImportRowError>()
        val firstByExternalId = mutableMapOf<String, Int>()
        val firstBySlug = mutableMapOf<String, Int>()

        rows.forEach { row ->
            firstByExternalId.putIfAbsent(row.externalId, row.rowNumber)?.let { firstRowNumber ->
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                    message = "Duplicate external_id '${row.externalId}' in rows $firstRowNumber and ${row.rowNumber}",
                )
            }

            firstBySlug.putIfAbsent(row.slug, row.rowNumber)?.let { firstRowNumber ->
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                    message = "Duplicate slug '${row.slug}' in rows $firstRowNumber and ${row.rowNumber}",
                )
            }
        }

        return errors
    }

    fun validateParentReferences(
        rows: List<CategoryImportRow>,
        existingExternalIds: Set<String>,
    ): List<CatalogImportRowError> {
        val errors = mutableListOf<CatalogImportRowError>()
        val fileExternalIds = rows.map { it.externalId }.toSet()
        val availableExternalIds = existingExternalIds + fileExternalIds

        rows.forEach { row ->
            val parentExternalId = row.parentExternalId ?: return@forEach

            if (parentExternalId == row.externalId) {
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.INVALID_RELATION,
                    message = "Category cannot reference itself as parent",
                )
                return@forEach
            }

            if (!availableExternalIds.contains(parentExternalId)) {
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.PARENT_CATEGORY_NOT_FOUND,
                    message = "Parent category with external_id '$parentExternalId' not found",
                )
            }
        }

        return errors
    }
}
