package ru.foodbox.delivery.modules.catalogimport.application

import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportExampleDescriptor
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportExampleFile
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType

interface CatalogImportExampleService {
    fun listExamples(): List<CatalogImportExampleDescriptor>
    fun getExample(importType: CatalogImportType, importMode: CatalogImportMode): CatalogImportExampleFile
}
