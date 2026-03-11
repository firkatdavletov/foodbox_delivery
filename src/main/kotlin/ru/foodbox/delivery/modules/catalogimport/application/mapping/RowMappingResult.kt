package ru.foodbox.delivery.modules.catalogimport.application.mapping

import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportRowError

data class RowMappingResult<T>(
    val row: T?,
    val errors: List<CatalogImportRowError>,
)
