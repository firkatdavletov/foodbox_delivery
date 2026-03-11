package ru.foodbox.delivery.modules.catalogimport.api

import ru.foodbox.delivery.modules.catalogimport.api.dto.CatalogImportExampleResponse
import ru.foodbox.delivery.modules.catalogimport.api.dto.CatalogImportResponse
import ru.foodbox.delivery.modules.catalogimport.api.dto.CatalogImportRowErrorResponse
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportExampleDescriptor
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

internal fun CatalogImportExampleDescriptor.toResponse(): CatalogImportExampleResponse {
    return CatalogImportExampleResponse(
        importType = importType,
        importMode = importMode,
        fileName = fileName,
        downloadUrl = "/api/v1/admin/catalog-import/examples/$importType/$importMode",
    )
}
