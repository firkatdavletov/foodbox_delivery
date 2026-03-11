package ru.foodbox.delivery.modules.catalogimport.application.mapping

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.application.support.ImportValueParser
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductImportRow

@Component
class ProductCsvRowMapper(
    private val importValueParser: ImportValueParser,
) {

    fun map(csvRow: CsvRow): RowMappingResult<ProductImportRow> {
        val rowNumber = csvRow.rowNumber
        val externalId = csvRow.get("external_id").clean()
        val sku = csvRow.get("sku").clean()
        val name = csvRow.get("name").clean()
        val categoryExternalId = csvRow.get("category_external_id").clean()
        val rowKey = externalId ?: sku
        val errors = mutableListOf<CatalogImportRowError>()

        if (externalId == null) {
            errors += requiredFieldError(rowNumber, rowKey, "external_id")
        }
        if (sku == null) {
            errors += requiredFieldError(rowNumber, rowKey, "sku")
        }
        if (name == null) {
            errors += requiredFieldError(rowNumber, rowKey, "name")
        }
        if (categoryExternalId == null) {
            errors += requiredFieldError(rowNumber, rowKey, "category_external_id")
        }

        val priceMinor = importValueParser.parsePriceMinor(
            raw = csvRow.get("price"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "price",
            required = true,
            errors = errors,
        )

        val oldPriceMinor = importValueParser.parsePriceMinor(
            raw = csvRow.get("old_price"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "old_price",
            required = false,
            errors = errors,
        )

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
            row = ProductImportRow(
                rowNumber = rowNumber,
                externalId = externalId!!,
                sku = sku!!,
                name = name!!,
                slug = csvRow.get("slug").clean(),
                description = csvRow.get("description").clean(),
                categoryExternalId = categoryExternalId!!,
                priceMinor = priceMinor!!,
                oldPriceMinor = oldPriceMinor,
                brand = csvRow.get("brand").clean(),
                isActive = isActive,
                imageUrl = csvRow.get("image_url").clean(),
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
