package ru.foodbox.delivery.modules.catalogimport.domain.model

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import java.util.UUID

data class ProductImportRow(
    val rowNumber: Int,
    val productExternalId: String?,
    val productSlug: String,
    val productTitle: String,
    val categoryExternalId: String?,
    val categoryId: UUID?,
    val productDescription: String?,
    val productBrand: String?,
    val productImageUrl: String?,
    val productPriceMinor: Long?,
    val productOldPriceMinor: Long?,
    val productSku: String?,
    val productUnit: ProductUnit,
    val productCountStep: Int,
    val productIsActive: Boolean,
    val productSortOrder: Int,
    val variantExternalId: String?,
    val variantSku: String?,
    val variantTitle: String?,
    val variantPriceMinor: Long?,
    val variantOldPriceMinor: Long?,
    val variantImageUrl: String?,
    val variantSortOrder: Int,
    val variantIsActive: Boolean,
    val options: List<ProductImportVariantOption>,
) {
    val productKey: String
        get() = productExternalId ?: productSlug

    val rowKey: String
        get() = productExternalId ?: variantExternalId ?: variantSku ?: productSlug

    fun hasVariantData(): Boolean {
        return !variantSku.isNullOrBlank() ||
            !variantExternalId.isNullOrBlank() ||
            options.isNotEmpty()
    }
}

data class ProductImportVariantOption(
    val position: Int,
    val optionGroupCode: String,
    val optionGroupTitle: String,
    val optionValueCode: String,
    val optionValueTitle: String,
)
