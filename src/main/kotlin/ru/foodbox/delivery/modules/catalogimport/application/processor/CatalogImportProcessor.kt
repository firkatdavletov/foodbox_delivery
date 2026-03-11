package ru.foodbox.delivery.modules.catalogimport.application.processor

import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow

interface CatalogImportProcessor {
    fun importType(): CatalogImportType
    fun process(rows: List<CsvRow>, mode: CatalogImportMode): CatalogImportReport
}
