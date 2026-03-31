package ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.application.mapping

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.application.mapping.RowMappingResult
import ru.foodbox.delivery.modules.catalogimport.application.support.ImportValueParser
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.domain.model.ProductModifierGroupLinkImportRow

@Component
class ProductModifierGroupLinkCsvRowMapper(
    private val importValueParser: ImportValueParser,
) {

    fun map(csvRow: CsvRow): RowMappingResult<ProductModifierGroupLinkImportRow> {
        val rowNumber = csvRow.rowNumber
        val productExternalId = readAny(csvRow, "product_external_id")
        val groupCode = readAny(csvRow, "group_code")
        val rowKey = buildRowKey(productExternalId, groupCode)
        val errors = mutableListOf<CatalogImportRowError>()

        if (productExternalId == null) {
            errors += requiredFieldError(rowNumber, rowKey, "product_external_id")
        }
        if (groupCode == null) {
            errors += requiredFieldError(rowNumber, rowKey, "group_code")
        }

        val isActive = importValueParser.parseBoolean(
            raw = readRawAny(csvRow, "is_active"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "is_active",
            defaultValue = true,
            errors = errors,
        )
        val sortOrder = importValueParser.parseInt(
            raw = readRawAny(csvRow, "sort_order"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "sort_order",
            defaultValue = 0,
            errors = errors,
        )

        if (errors.isNotEmpty()) {
            return RowMappingResult(row = null, errors = errors)
        }

        return RowMappingResult(
            row = ProductModifierGroupLinkImportRow(
                rowNumber = rowNumber,
                productExternalId = productExternalId!!,
                groupCode = groupCode!!,
                isActive = isActive,
                sortOrder = sortOrder,
            ),
            errors = emptyList(),
        )
    }

    private fun buildRowKey(productExternalId: String?, groupCode: String?): String? {
        return when {
            productExternalId != null && groupCode != null -> "$productExternalId::$groupCode"
            productExternalId != null -> productExternalId
            else -> groupCode
        }
    }

    private fun requiredFieldError(rowNumber: Int, rowKey: String?, fieldName: String): CatalogImportRowError {
        return CatalogImportRowError(
            rowNumber = rowNumber,
            rowKey = rowKey,
            errorCode = CatalogImportErrorCode.MISSING_REQUIRED_FIELD,
            message = "Field '$fieldName' is required",
        )
    }

    private fun readAny(csvRow: CsvRow, vararg headers: String): String? {
        return headers.asSequence()
            .mapNotNull { header -> csvRow.get(header).clean() }
            .firstOrNull()
    }

    private fun readRawAny(csvRow: CsvRow, vararg headers: String): String? {
        return headers.asSequence()
            .mapNotNull { header -> csvRow.get(header) }
            .firstOrNull()
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotBlank() }
}
