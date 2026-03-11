package ru.foodbox.delivery.modules.catalogimport.application.support

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import java.math.BigDecimal

@Component
class ImportValueParser {

    fun parseBoolean(
        raw: String?,
        rowNumber: Int,
        rowKey: String?,
        fieldName: String,
        defaultValue: Boolean,
        errors: MutableList<CatalogImportRowError>,
    ): Boolean {
        val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return defaultValue
        return when (value.lowercase()) {
            "true", "1", "yes", "y" -> true
            "false", "0", "no", "n" -> false
            else -> {
                errors += CatalogImportRowError(
                    rowNumber = rowNumber,
                    rowKey = rowKey,
                    errorCode = CatalogImportErrorCode.INVALID_BOOLEAN,
                    message = "Field '$fieldName' must be a boolean value",
                )
                defaultValue
            }
        }
    }

    fun parseInt(
        raw: String?,
        rowNumber: Int,
        rowKey: String?,
        fieldName: String,
        defaultValue: Int,
        errors: MutableList<CatalogImportRowError>,
    ): Int {
        val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return defaultValue
        return value.toIntOrNull() ?: run {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field '$fieldName' must be an integer number",
            )
            defaultValue
        }
    }

    fun parsePriceMinor(
        raw: String?,
        rowNumber: Int,
        rowKey: String?,
        fieldName: String,
        required: Boolean,
        errors: MutableList<CatalogImportRowError>,
    ): Long? {
        val value = raw?.trim()?.takeIf { it.isNotBlank() }
        if (value == null) {
            if (required) {
                errors += CatalogImportRowError(
                    rowNumber = rowNumber,
                    rowKey = rowKey,
                    errorCode = CatalogImportErrorCode.MISSING_REQUIRED_FIELD,
                    message = "Field '$fieldName' is required",
                )
            }
            return null
        }

        val decimal = value.replace(',', '.').toBigDecimalOrNull()
        if (decimal == null) {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field '$fieldName' must be a valid number",
            )
            return null
        }

        if (decimal < BigDecimal.ZERO) {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field '$fieldName' must not be negative",
            )
            return null
        }

        if (decimal.scale() > 2) {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field '$fieldName' must have at most 2 decimal places",
            )
            return null
        }

        return try {
            decimal.movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field '$fieldName' is out of range",
            )
            null
        }
    }
}
