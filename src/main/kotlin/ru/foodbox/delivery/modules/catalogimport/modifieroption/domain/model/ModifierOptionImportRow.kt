package ru.foodbox.delivery.modules.catalogimport.modifieroption.domain.model

import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType

data class ModifierOptionImportRow(
    val rowNumber: Int,
    val groupCode: String,
    val optionCode: String,
    val name: String,
    val description: String?,
    val priceType: ModifierPriceType,
    val price: Long,
    val applicationScope: ModifierApplicationScope,
    val isDefault: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
) {
    val rowKey: String
        get() = "$groupCode::$optionCode"
}
