package ru.foodbox.delivery.modules.catalogimport.modifiergroup.domain.model

data class ModifierGroupImportRow(
    val rowNumber: Int,
    val groupCode: String,
    val name: String,
    val minSelected: Int,
    val maxSelected: Int,
    val isRequired: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
) {
    val rowKey: String
        get() = groupCode
}
