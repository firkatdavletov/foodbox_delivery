package ru.foodbox.delivery.modules.catalogimport.domain

data class CatalogImportExampleDescriptor(
    val importType: CatalogImportType,
    val importMode: CatalogImportMode,
    val fileName: String,
)
