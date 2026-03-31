package ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.application.validation

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.domain.model.ProductModifierGroupLinkImportRow

@Component
class ProductModifierGroupLinkImportRowValidator {

    fun validateDuplicates(rows: List<ProductModifierGroupLinkImportRow>): List<CatalogImportRowError> {
        val errors = mutableListOf<CatalogImportRowError>()
        val firstRowByKey = mutableMapOf<String, Int>()

        rows.forEach { row ->
            firstRowByKey.putIfAbsent(row.rowKey, row.rowNumber)?.let { firstRowNumber ->
                errors += CatalogImportRowError(
                    rowNumber = row.rowNumber,
                    rowKey = row.rowKey,
                    errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                    message = "Duplicate product/group key '${row.rowKey}' in rows $firstRowNumber and ${row.rowNumber}",
                )
            }
        }

        return errors
    }

    fun validateProductReferences(
        rows: List<ProductModifierGroupLinkImportRow>,
        existingProductExternalIds: Set<String>,
    ): List<CatalogImportRowError> {
        return rows.mapNotNull { row ->
            if (existingProductExternalIds.contains(row.productExternalId)) {
                return@mapNotNull null
            }

            CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.PRODUCT_NOT_FOUND,
                message = "Product '${row.productExternalId}' not found",
            )
        }
    }

    fun validateGroupReferences(
        rows: List<ProductModifierGroupLinkImportRow>,
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
}
