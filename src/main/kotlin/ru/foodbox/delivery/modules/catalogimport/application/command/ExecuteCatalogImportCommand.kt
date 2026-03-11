package ru.foodbox.delivery.modules.catalogimport.application.command

import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType

data class ExecuteCatalogImportCommand(
    val importType: CatalogImportType,
    val importMode: CatalogImportMode,
    val csvBytes: ByteArray,
)
