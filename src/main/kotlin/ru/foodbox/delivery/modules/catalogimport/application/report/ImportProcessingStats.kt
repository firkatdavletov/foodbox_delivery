package ru.foodbox.delivery.modules.catalogimport.application.report

import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError

data class ImportProcessingStats(
    var successCount: Int = 0,
    var createdCount: Int = 0,
    var updatedCount: Int = 0,
    var skippedCount: Int = 0,
    val rowErrors: MutableList<CatalogImportRowError> = mutableListOf(),
)
