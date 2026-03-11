package ru.foodbox.delivery.modules.catalogimport.api.dto

import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType

data class CatalogImportExampleResponse(
    val importType: CatalogImportType,
    val importMode: CatalogImportMode,
    val fileName: String,
    val downloadUrl: String,
)
