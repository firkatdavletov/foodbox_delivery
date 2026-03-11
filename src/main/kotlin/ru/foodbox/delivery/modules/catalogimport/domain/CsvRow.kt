package ru.foodbox.delivery.modules.catalogimport.domain

data class CsvRow(
    val rowNumber: Int,
    val values: Map<String, String>,
) {
    fun get(headerName: String): String? = values[headerName.lowercase()]
}
