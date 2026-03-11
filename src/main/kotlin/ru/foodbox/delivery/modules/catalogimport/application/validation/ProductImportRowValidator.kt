package ru.foodbox.delivery.modules.catalogimport.application.validation

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductImportRow

@Component
class ProductImportRowValidator {

    fun validateDuplicates(rows: List<ProductImportRow>): List<CatalogImportRowError> {
        val errors = mutableListOf<CatalogImportRowError>()
        val firstByExternalId = mutableMapOf<String, Int>()
        val firstBySku = mutableMapOf<String, Int>()

        rows.forEach { row ->
            firstByExternalId.putIfAbsent(row.externalId, row.rowNumber)?.let { firstRowNumber ->
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                    message = "Duplicate external_id '${row.externalId}' in rows $firstRowNumber and ${row.rowNumber}",
                )
            }

            firstBySku.putIfAbsent(row.sku, row.rowNumber)?.let { firstRowNumber ->
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.externalId,
                    errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                    message = "Duplicate sku '${row.sku}' in rows $firstRowNumber and ${row.rowNumber}",
                )
            }
        }

        return errors
    }

    fun validateCategoryReferences(
        rows: List<ProductImportRow>,
        existingCategoryExternalIds: Set<String>,
    ): List<CatalogImportRowError> {
        return rows.mapNotNull { row ->
            if (existingCategoryExternalIds.contains(row.categoryExternalId)) {
                return@mapNotNull null
            }

            CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.externalId,
                errorCode = CatalogImportErrorCode.CATEGORY_NOT_FOUND,
                message = "Category with external_id '${row.categoryExternalId}' not found",
            )
        }
    }
}
