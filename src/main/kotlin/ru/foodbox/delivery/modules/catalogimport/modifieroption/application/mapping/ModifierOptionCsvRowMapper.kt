package ru.foodbox.delivery.modules.catalogimport.modifieroption.application.mapping

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import ru.foodbox.delivery.modules.catalogimport.application.mapping.RowMappingResult
import ru.foodbox.delivery.modules.catalogimport.application.support.ImportValueParser
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.modifieroption.domain.model.ModifierOptionImportRow

@Component
class ModifierOptionCsvRowMapper(
    private val importValueParser: ImportValueParser,
) {

    fun map(csvRow: CsvRow): RowMappingResult<ModifierOptionImportRow> {
        val rowNumber = csvRow.rowNumber
        val groupCode = readAny(csvRow, "group_code")
        val optionCode = readAny(csvRow, "option_code")
        val name = readAny(csvRow, "name")
        val rowKey = buildRowKey(groupCode, optionCode)
        val errors = mutableListOf<CatalogImportRowError>()

        if (groupCode == null) {
            errors += requiredFieldError(rowNumber, rowKey, "group_code")
        }
        if (optionCode == null) {
            errors += requiredFieldError(rowNumber, rowKey, "option_code")
        }
        if (name == null) {
            errors += requiredFieldError(rowNumber, rowKey, "name")
        }

        val priceType = parseRequiredEnum<ModifierPriceType>(
            csvRow = csvRow,
            header = "price_type",
            rowNumber = rowNumber,
            rowKey = rowKey,
            errors = errors,
        )
        val applicationScope = parseRequiredEnum<ModifierApplicationScope>(
            csvRow = csvRow,
            header = "application_scope",
            rowNumber = rowNumber,
            rowKey = rowKey,
            errors = errors,
        )
        val price = parsePrice(
            csvRow = csvRow,
            priceType = priceType,
            rowNumber = rowNumber,
            rowKey = rowKey,
            errors = errors,
        )
        val isDefault = importValueParser.parseBoolean(
            raw = readRawAny(csvRow, "is_default"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "is_default",
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
            row = ModifierOptionImportRow(
                rowNumber = rowNumber,
                groupCode = groupCode!!,
                optionCode = optionCode!!,
                name = name!!,
                description = readAny(csvRow, "description"),
                priceType = priceType!!,
                price = price!!,
                applicationScope = applicationScope!!,
                isDefault = isDefault,
                isActive = isActive,
                sortOrder = sortOrder,
            ),
            errors = emptyList(),
        )
    }

    private inline fun <reified T : Enum<T>> parseRequiredEnum(
        csvRow: CsvRow,
        header: String,
        rowNumber: Int,
        rowKey: String?,
        errors: MutableList<CatalogImportRowError>,
    ): T? {
        val raw = readRawAny(csvRow, header)
        if (raw.isNullOrBlank()) {
            errors += requiredFieldError(rowNumber, rowKey, header)
            return null
        }
        return importValueParser.parseEnum<T>(
            raw = raw,
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = header,
            values = enumValues<T>(),
            errors = errors,
        )
    }

    private fun parsePrice(
        csvRow: CsvRow,
        priceType: ModifierPriceType?,
        rowNumber: Int,
        rowKey: String?,
        errors: MutableList<CatalogImportRowError>,
    ): Long? {
        val raw = readRawAny(csvRow, "price")
        if (raw.isNullOrBlank()) {
            if (priceType == ModifierPriceType.FREE) {
                return 0L
            }
            errors += requiredFieldError(rowNumber, rowKey, "price")
            return null
        }

        return importValueParser.parsePriceMinor(
            raw = raw,
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "price",
            required = true,
            errors = errors,
        )
    }

    private fun buildRowKey(groupCode: String?, optionCode: String?): String? {
        return when {
            groupCode != null && optionCode != null -> "$groupCode::$optionCode"
            groupCode != null -> groupCode
            else -> optionCode
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
