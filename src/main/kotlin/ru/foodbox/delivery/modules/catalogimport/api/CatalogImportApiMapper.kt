package ru.foodbox.delivery.modules.catalogimport.api

import ru.foodbox.delivery.modules.catalogimport.api.dto.CatalogImportResponse
import ru.foodbox.delivery.modules.catalogimport.api.dto.CatalogImportRowErrorResponse
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport

internal fun CatalogImportReport.toResponse(): CatalogImportResponse {
    return CatalogImportResponse(
        importType = importType,
        importMode = importMode,
        totalRows = totalRows,
        successCount = successCount,
        createdCount = createdCount,
        updatedCount = updatedCount,
        skippedCount = skippedCount,
        errorCount = errorCount,
        rowErrors = rowErrors.map { rowError ->
            CatalogImportRowErrorResponse(
                rowNumber = rowError.rowNumber,
                rowKey = rowError.rowKey,
                errorCode = rowError.errorCode.name,
                message = rowError.message,
            )
        },
    )
}
