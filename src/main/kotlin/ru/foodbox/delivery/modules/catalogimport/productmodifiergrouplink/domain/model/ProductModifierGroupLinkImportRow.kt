package ru.foodbox.delivery.modules.catalogimport.productmodifiergrouplink.domain.model

data class ProductModifierGroupLinkImportRow(
    val rowNumber: Int,
    val productExternalId: String,
    val groupCode: String,
    val isActive: Boolean,
    val sortOrder: Int,
) {
    val rowKey: String
        get() = "$productExternalId::$groupCode"
}
