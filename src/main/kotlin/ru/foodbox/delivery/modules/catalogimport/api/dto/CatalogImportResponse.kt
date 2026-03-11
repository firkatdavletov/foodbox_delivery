package ru.foodbox.delivery.modules.catalogimport.api.dto

import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType

data class CatalogImportResponse(
    val importType: CatalogImportType,
    val importMode: CatalogImportMode,
    val totalRows: Int,
    val successCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val rowErrors: List<CatalogImportRowErrorResponse>,
)
