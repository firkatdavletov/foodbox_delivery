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
        val externalId = csvRow.get("external_id").clean()
        val name = csvRow.get("name").clean()
        val slugRaw = csvRow.get("slug").clean()
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
            raw = csvRow.get("is_active"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "is_active",
            defaultValue = true,
            errors = errors,
        )

        val sortOrder = importValueParser.parseInt(
            raw = csvRow.get("sort_order"),
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
                parentExternalId = csvRow.get("parent_external_id").clean(),
                description = csvRow.get("description").clean(),
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

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotBlank() }
}
