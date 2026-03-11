package ru.foodbox.delivery.modules.catalogimport.application.report

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType

@Component
class CatalogImportReportBuilder {

    fun build(
        importType: CatalogImportType,
        importMode: CatalogImportMode,
        totalRows: Int,
        stats: ImportProcessingStats,
    ): CatalogImportReport {
        val sortedErrors = stats.rowErrors.sortedBy { it.rowNumber }
        return CatalogImportReport(
            importType = importType,
            importMode = importMode,
            totalRows = totalRows,
            successCount = stats.successCount,
            createdCount = stats.createdCount,
            updatedCount = stats.updatedCount,
            skippedCount = stats.skippedCount,
            errorCount = sortedErrors.size,
            rowErrors = sortedErrors,
        )
    }
}
