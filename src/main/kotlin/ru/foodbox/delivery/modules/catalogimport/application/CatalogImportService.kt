package ru.foodbox.delivery.modules.catalogimport.application

import ru.foodbox.delivery.modules.catalogimport.application.command.ExecuteCatalogImportCommand
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport

interface CatalogImportService {
    fun execute(command: ExecuteCatalogImportCommand): CatalogImportReport
}
