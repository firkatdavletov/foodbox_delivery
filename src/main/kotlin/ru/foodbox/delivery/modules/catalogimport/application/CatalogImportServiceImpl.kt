package ru.foodbox.delivery.modules.catalogimport.application

import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.catalogimport.application.command.ExecuteCatalogImportCommand
import ru.foodbox.delivery.modules.catalogimport.application.processor.CatalogImportProcessor
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportReport
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType
import ru.foodbox.delivery.modules.catalogimport.infrastructure.csv.CsvCatalogParser

@Service
class CatalogImportServiceImpl(
    private val csvCatalogParser: CsvCatalogParser,
    processors: List<CatalogImportProcessor>,
) : CatalogImportService {

    private val processorByType: Map<CatalogImportType, CatalogImportProcessor> = processors.associateBy { it.importType() }

    override fun execute(command: ExecuteCatalogImportCommand): CatalogImportReport {
        if (command.csvBytes.isEmpty()) {
            throw IllegalArgumentException("CSV file is empty")
        }

        val rows = csvCatalogParser.parse(command.csvBytes)
        val processor = processorByType[command.importType]
            ?: throw IllegalArgumentException("Unsupported import type: ${command.importType}")

        return processor.process(rows, command.importMode)
    }
}
