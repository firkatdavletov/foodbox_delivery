package ru.foodbox.delivery.modules.catalogimport.application.mapping

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.application.support.ImportValueParser
import ru.foodbox.delivery.modules.catalogimport.application.support.SlugNormalizer
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.domain.model.CategoryImportRow

@Component
class CategoryCsvRowMapper(
    private val importValueParser: ImportValueParser,
    private val slugNormalizer: SlugNormalizer,
) {

    fun map(csvRow: CsvRow): RowMappingResult<CategoryImportRow> {
        val rowNumber = csvRow.rowNumber
        val externalId = readAny(
            csvRow,
            "external_id",
            "category_external_id",
            "внешний id категории",
            "внешний id категории в каталоге",
        )
        val name = readAny(csvRow, "name", "название категории")
        val slugRaw = readAny(csvRow, "slug", "слаг категории")
        val rowKey = externalId ?: slugRaw
        val errors = mutableListOf<CatalogImportRowError>()

        if (externalId == null) {
            errors += requiredFieldError(rowNumber, rowKey, "external_id")
        }
        if (name == null) {
            errors += requiredFieldError(rowNumber, rowKey, "name")
        }
        if (slugRaw == null) {
            errors += requiredFieldError(rowNumber, rowKey, "slug")
        }

        val isActive = importValueParser.parseBoolean(
            raw = readRawAny(csvRow, "is_active", "категория активна"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "is_active",
            defaultValue = true,
            errors = errors,
        )

        val sortOrder = importValueParser.parseInt(
            raw = readRawAny(csvRow, "sort_order", "порядок сортировки категории"),
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
            row = CategoryImportRow(
                rowNumber = rowNumber,
                externalId = externalId!!,
                name = name!!,
                slug = slugNormalizer.normalize(slugRaw, name),
                parentExternalId = readAny(csvRow, "parent_external_id", "внешний id родительской категории"),
                description = readAny(csvRow, "description", "описание категории"),
                isActive = isActive,
                sortOrder = sortOrder,
            ),
            errors = emptyList(),
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
