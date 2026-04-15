package ru.foodbox.delivery.modules.catalogimport.application.validation

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportErrorCode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductImportRow
import ru.foodbox.delivery.modules.catalogimport.domain.model.ProductRowType
import java.util.UUID

@Component
class ProductImportRowValidator {

    fun validateDuplicates(rows: List<ProductImportRow>): List<CatalogImportRowError> {
        val errors = mutableListOf<CatalogImportRowError>()
        val firstVariantSkuRow = mutableMapOf<String, Int>()
        val firstProductSkuRow = mutableMapOf<String, Int>()

        rows.groupBy { it.productKey }.forEach { (_, groupedRows) ->
            val hasVariants = groupedRows.any(ProductImportRow::hasVariantData)

            if (hasVariants) {
                val firstByVariantKey = mutableMapOf<String, Int>()
                groupedRows.forEach rowLoop@{ row ->
                    // New-style product header rows (row_type=product without variant data) are allowed alongside variant rows
                    if (row.rowType == ProductRowType.PRODUCT && !row.hasVariantData()) {
                        return@rowLoop
                    }
                    if (!row.hasVariantData()) {
                        errors += CatalogImportRowError(
                            rowNumber = row.rowNumber,
                            rowKey = row.rowKey,
                            errorCode = CatalogImportErrorCode.INVALID_RELATION,
                            message = "Mixed simple and variant rows for product '${row.productKey}'",
                        )
                        return@rowLoop
                    }

                    val variantSku = row.variantSku
                    if (variantSku.isNullOrBlank()) {
                        errors += CatalogImportRowError(
                            rowNumber = row.rowNumber,
                            rowKey = row.rowKey,
                            errorCode = CatalogImportErrorCode.MISSING_REQUIRED_FIELD,
                            message = "Field 'variant_sku' is required for variant rows",
                        )
                    } else {
                        firstVariantSkuRow.putIfAbsent(variantSku, row.rowNumber)?.let { firstRowNumber ->
                            errors += CatalogImportRowError(
                                rowNumber = row.rowNumber,
                                rowKey = row.rowKey,
                                errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                                message = "Duplicate variant_sku '$variantSku' in rows $firstRowNumber and ${row.rowNumber}",
                            )
                        }
                    }

                    val variantKey = row.variantExternalId ?: row.variantSku
                    if (variantKey.isNullOrBlank()) {
                        errors += CatalogImportRowError(
                            rowNumber = row.rowNumber,
                            rowKey = row.rowKey,
                            errorCode = CatalogImportErrorCode.MISSING_REQUIRED_FIELD,
                            message = "Either 'variant_external_id' or 'variant_sku' is required for variant rows",
                        )
                    } else {
                        firstByVariantKey.putIfAbsent(variantKey, row.rowNumber)?.let { firstRowNumber ->
                            errors += CatalogImportRowError(
                                rowNumber = row.rowNumber,
                                rowKey = row.rowKey,
                                errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                                message = "Duplicate variant key '$variantKey' in rows $firstRowNumber and ${row.rowNumber}",
                            )
                        }
                    }
                }
            } else {
                if (groupedRows.size > 1) {
                    groupedRows.drop(1).forEach { row ->
                        errors += CatalogImportRowError(
                            rowNumber = row.rowNumber,
                            rowKey = row.rowKey,
                            errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                            message = "Duplicate simple product row for key '${row.productKey}'",
                        )
                    }
                }

                groupedRows.firstOrNull()?.productSku?.let { productSku ->
                    firstProductSkuRow.putIfAbsent(productSku, groupedRows.first().rowNumber)?.let { firstRowNumber ->
                        errors += CatalogImportRowError(
                            rowNumber = groupedRows.first().rowNumber,
                            rowKey = groupedRows.first().rowKey,
                            errorCode = CatalogImportErrorCode.DUPLICATE_KEY_IN_FILE,
                            message = "Duplicate product sku '$productSku' in rows $firstRowNumber and ${groupedRows.first().rowNumber}",
                        )
                    }
                }
            }
        }

        return errors
    }

    fun validateCategoryReferences(
        rows: List<ProductImportRow>,
        existingCategoryExternalIds: Set<String>,
        existingCategoryIds: Set<UUID>,
    ): List<CatalogImportRowError> {
        return rows.mapNotNull { row ->
            // Variant rows don't carry category — it belongs to the product row
            if (row.rowType == ProductRowType.VARIANT) return@mapNotNull null

            val hasExternal = row.categoryExternalId?.let(existingCategoryExternalIds::contains) ?: false
            val hasId = row.categoryId?.let(existingCategoryIds::contains) ?: false

            if (hasExternal || hasId) {
                return@mapNotNull null
            }

            val missingRef = row.categoryExternalId ?: row.categoryId?.toString()
            CatalogImportRowError(
                rowNumber = row.rowNumber,
                rowKey = row.rowKey,
                errorCode = CatalogImportErrorCode.CATEGORY_NOT_FOUND,
                message = "Category reference '$missingRef' not found",
            )
        }
    }
}
