package ru.foodbox.delivery.modules.catalogimport.modifiergroup.application.mapping

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.application.mapping.RowMappingResult
import ru.foodbox.delivery.modules.catalogimport.application.support.ImportValueParser
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.modifiergroup.domain.model.ModifierGroupImportRow

@Component
class ModifierGroupCsvRowMapper(
    private val importValueParser: ImportValueParser,
) {

    fun map(csvRow: CsvRow): RowMappingResult<ModifierGroupImportRow> {
        val rowNumber = csvRow.rowNumber
        val groupCode = readAny(csvRow, "group_code")
        val name = readAny(csvRow, "name")
        val rowKey = groupCode ?: name
        val errors = mutableListOf<CatalogImportRowError>()

        if (groupCode == null) {
            errors += requiredFieldError(rowNumber, rowKey, "group_code")
        }
        if (name == null) {
            errors += requiredFieldError(rowNumber, rowKey, "name")
        }

        val minSelected = parseRequiredInt(
            csvRow = csvRow,
            header = "min_selected",
            rowNumber = rowNumber,
            rowKey = rowKey,
            errors = errors,
        )
        val maxSelected = parseRequiredInt(
            csvRow = csvRow,
            header = "max_selected",
            rowNumber = rowNumber,
            rowKey = rowKey,
            errors = errors,
        )
        val isRequired = importValueParser.parseBoolean(
            raw = readRawAny(csvRow, "is_required"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "is_required",
            defaultValue = false,
            errors = errors,
        )
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
            row = ModifierGroupImportRow(
                rowNumber = rowNumber,
                groupCode = groupCode!!,
                name = name!!,
                minSelected = minSelected!!,
                maxSelected = maxSelected!!,
                isRequired = isRequired,
                isActive = isActive,
                sortOrder = sortOrder,
            ),
            errors = emptyList(),
        )
    }

    private fun parseRequiredInt(
        csvRow: CsvRow,
        header: String,
        rowNumber: Int,
        rowKey: String?,
        errors: MutableList<CatalogImportRowError>,
    ): Int? {
        val raw = readRawAny(csvRow, header)
        if (raw.isNullOrBlank()) {
            errors += requiredFieldError(rowNumber, rowKey, header)
            return null
        }
        return importValueParser.parseInt(
            raw = raw,
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = header,
            defaultValue = 0,
            errors = errors,
        )
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
