package ru.foodbox.delivery.modules.catalogimport.application.mapping

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalogimport.application.support.ImportValueParser
import ru.foodbox.delivery.modules.catalogimport.application.support.SlugNormalizer
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductImportRow
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductImportVariantOption
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductRowType
import java.util.UUID

@Component
class ProductCsvRowMapper(
    private val importValueParser: ImportValueParser,
    private val slugNormalizer: SlugNormalizer,
) {

    fun map(csvRow: CsvRow): RowMappingResult<ProductImportRow> {
        val rowNumber = csvRow.rowNumber
        val rowType = when (readAny(csvRow, "row_type")?.lowercase()) {
            "variant" -> ProductRowType.VARIANT
            else -> ProductRowType.PRODUCT
        }
        val productExternalId = readAny(csvRow, "product_external_id", "external_id")
        val productTitle = readAny(csvRow, "product_title", "name")
        val productSlugRaw = readAny(csvRow, "product_slug", "slug")
        val variantExternalId = readAny(csvRow, "variant_external_id")
        val variantSku = readAny(csvRow, "variant_sku")
        val fallbackRowKey = productExternalId ?: variantExternalId ?: variantSku ?: productSlugRaw
        val errors = mutableListOf<CatalogImportRowError>()

        if (rowType == ProductRowType.PRODUCT && productTitle == null) {
            errors += requiredFieldError(rowNumber, fallbackRowKey, "product_title")
        }

        if (rowType == ProductRowType.VARIANT && productExternalId == null) {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = fallbackRowKey,
                errorCode = CatalogImportErrorCode.MISSING_REQUIRED_FIELD,
                message = "Field 'product_external_id' is required for variant rows (row_type=variant)",
            )
        }

        val rowKey = productExternalId ?: variantExternalId ?: variantSku ?: productSlugRaw ?: productTitle

        val categoryExternalId = readAny(csvRow, "category_external_id")
        val categoryId = parseCategoryId(csvRow, rowNumber, rowKey, errors)
        if (rowType == ProductRowType.PRODUCT && categoryExternalId == null && categoryId == null) {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = rowKey,
                errorCode = CatalogImportErrorCode.MISSING_REQUIRED_FIELD,
                message = "Either 'category_external_id' or 'category_id' is required",
            )
        }

        val productPriceMinor = parseMinorAmount(
            csvRow = csvRow,
            minorHeader = "product_price_minor",
            decimalHeader = "price",
            rowNumber = rowNumber,
            rowKey = rowKey,
            required = false,
            errors = errors,
        )

        val productOldPriceMinor = parseMinorAmount(
            csvRow = csvRow,
            minorHeader = "product_old_price_minor",
            decimalHeader = "old_price",
            rowNumber = rowNumber,
            rowKey = rowKey,
            required = false,
            errors = errors,
        )

        val variantPriceMinor = parseMinorAmount(
            csvRow = csvRow,
            minorHeader = "variant_price_minor",
            decimalHeader = null,
            rowNumber = rowNumber,
            rowKey = rowKey,
            required = false,
            errors = errors,
        )

        val variantOldPriceMinor = parseMinorAmount(
            csvRow = csvRow,
            minorHeader = "variant_old_price_minor",
            decimalHeader = null,
            rowNumber = rowNumber,
            rowKey = rowKey,
            required = false,
            errors = errors,
        )

        val productUnit = parseProductUnit(csvRow, rowNumber, rowKey, errors)

        val productCountStep = importValueParser.parseInt(
            raw = readRawAny(csvRow, "product_count_step"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "product_count_step",
            defaultValue = 1,
            errors = errors,
        )
        if (productCountStep <= 0) {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field 'product_count_step' must be greater than zero",
            )
        }

        val productIsActive = importValueParser.parseBoolean(
            raw = readRawAny(csvRow, "product_is_active", "is_active"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "product_is_active",
            defaultValue = true,
            errors = errors,
        )

        val productSortOrder = importValueParser.parseInt(
            raw = readRawAny(csvRow, "product_sort_order", "sort_order"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "product_sort_order",
            defaultValue = 0,
            errors = errors,
        )

        val variantSortOrder = importValueParser.parseInt(
            raw = readRawAny(csvRow, "variant_sort_order"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "variant_sort_order",
            defaultValue = 0,
            errors = errors,
        )

        val variantIsActive = importValueParser.parseBoolean(
            raw = readRawAny(csvRow, "variant_is_active"),
            rowNumber = rowNumber,
            rowKey = rowKey,
            fieldName = "variant_is_active",
            defaultValue = true,
            errors = errors,
        )

        val options = parseVariantOptions(csvRow, rowNumber, rowKey, errors)

        if (errors.isNotEmpty()) {
            return RowMappingResult(row = null, errors = errors)
        }

        val normalizedSlug = if (rowType == ProductRowType.PRODUCT) {
            slugNormalizer.normalize(productSlugRaw, productTitle!!)
        } else {
            ""
        }

        return RowMappingResult(
            row = ProductImportRow(
                rowNumber = rowNumber,
                rowType = rowType,
                productExternalId = productExternalId,
                productSlug = normalizedSlug,
                productTitle = productTitle,
                categoryExternalId = categoryExternalId,
                categoryId = categoryId,
                productDescription = readAny(csvRow, "product_description", "description"),
                productBrand = readAny(csvRow, "product_brand", "brand"),
                productImageUrl = readAny(csvRow, "product_image_url", "image_url"),
                productPriceMinor = productPriceMinor,
                productOldPriceMinor = productOldPriceMinor,
                productSku = readAny(csvRow, "product_sku", "sku"),
                productUnit = productUnit,
                productCountStep = productCountStep,
                productIsActive = productIsActive,
                productSortOrder = productSortOrder,
                variantExternalId = variantExternalId,
                variantSku = variantSku,
                variantTitle = readAny(csvRow, "variant_title"),
                variantPriceMinor = variantPriceMinor,
                variantOldPriceMinor = variantOldPriceMinor,
                variantImageUrl = readAny(csvRow, "variant_image_url"),
                variantSortOrder = variantSortOrder,
                variantIsActive = variantIsActive,
                options = options,
            ),
            errors = emptyList(),
        )
    }

    private fun parseCategoryId(
        csvRow: CsvRow,
        rowNumber: Int,
        rowKey: String?,
        errors: MutableList<CatalogImportRowError>,
    ): UUID? {
        val rawCategoryId = readAny(csvRow, "category_id") ?: return null
        return try {
            UUID.fromString(rawCategoryId)
        } catch (_: IllegalArgumentException) {
            errors += CatalogImportRowError(
                rowNumber = rowNumber,
                rowKey = rowKey,
                errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                message = "Field 'category_id' must be a valid UUID",
            )
            null
        }
    }

    private fun parseMinorAmount(
        csvRow: CsvRow,
        minorHeader: String,
        decimalHeader: String?,
        rowNumber: Int,
        rowKey: String?,
        required: Boolean,
        errors: MutableList<CatalogImportRowError>,
    ): Long? {
        val rawMinor = readAny(csvRow, minorHeader)
        if (rawMinor != null) {
            val value = rawMinor.toLongOrNull()
            if (value == null || value < 0) {
                errors += CatalogImportRowError(
                    rowNumber = rowNumber,
                    rowKey = rowKey,
                    errorCode = CatalogImportErrorCode.INVALID_NUMBER,
                    message = "Field '$minorHeader' must be a non-negative integer in minor units",
                )
                return null
            }
            return value
        }

        if (decimalHeader != null) {
            return importValueParser.parsePriceMinor(
                raw = readRawAny(csvRow, decimalHeader),
                rowNumber = rowNumber,
                rowKey = rowKey,
                fieldName = decimalHeader,
                required = required,
                errors = errors,
            )
        }

        return null
    }

    private fun parseProductUnit(
        csvRow: CsvRow,
        rowNumber: Int,
        rowKey: String?,
        errors: MutableList<CatalogImportRowError>,
    ): ProductUnit {
        val rawUnit = readAny(csvRow, "product_unit") ?: return ProductUnit.PIECE
        return runCatching { ProductUnit.valueOf(rawUnit.uppercase()) }
            .getOrElse {
                errors += CatalogImportRowError(
                    rowNumber = rowNumber,
                    rowKey = rowKey,
                    errorCode = CatalogImportErrorCode.INVALID_RELATION,
                    message = "Field 'product_unit' has unsupported value '$rawUnit'",
                )
                ProductUnit.PIECE
            }
    }

    private fun parseVariantOptions(
        csvRow: CsvRow,
        rowNumber: Int,
        rowKey: String?,
        errors: MutableList<CatalogImportRowError>,
    ): List<ProductImportVariantOption> {
        val positions = csvRow.values.keys
            .mapNotNull { OPTION_GROUP_CODE_REGEX.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            .sorted()

        return positions.mapNotNull { position ->
            val groupCode = readAny(csvRow, "option${position}_group_code")
            val groupTitle = readAny(csvRow, "option${position}_group_title")
            val valueCode = readAny(csvRow, "option${position}_value_code")
            val valueTitle = readAny(csvRow, "option${position}_value_title")

            if (groupCode == null && groupTitle == null && valueCode == null && valueTitle == null) {
                return@mapNotNull null
            }

            if (groupCode == null) {
                errors += requiredFieldError(rowNumber, rowKey, "option${position}_group_code")
            }
            if (groupTitle == null) {
                errors += requiredFieldError(rowNumber, rowKey, "option${position}_group_title")
            }
            if (valueCode == null) {
                errors += requiredFieldError(rowNumber, rowKey, "option${position}_value_code")
            }
            if (valueTitle == null) {
                errors += requiredFieldError(rowNumber, rowKey, "option${position}_value_title")
            }

            if (groupCode == null || groupTitle == null || valueCode == null || valueTitle == null) {
                return@mapNotNull null
            }

            ProductImportVariantOption(
                position = position,
                optionGroupCode = groupCode,
                optionGroupTitle = groupTitle,
                optionValueCode = valueCode,
                optionValueTitle = valueTitle,
            )
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

    private companion object {
        val OPTION_GROUP_CODE_REGEX = Regex("option(\\d+)_group_code")
    }
}
