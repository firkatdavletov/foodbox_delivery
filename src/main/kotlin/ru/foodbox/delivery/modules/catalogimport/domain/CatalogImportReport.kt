package ru.foodbox.delivery.modules.catalogimport.domain

data class CatalogImportReport(
    val importType: CatalogImportType,
    val importMode: CatalogImportMode,
    val totalRows: Int,
    val successCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val rowErrors: List<CatalogImportRowError>,
)
