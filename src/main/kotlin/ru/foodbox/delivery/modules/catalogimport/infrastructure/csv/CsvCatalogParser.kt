package ru.foodbox.delivery.modules.catalogimport.infrastructure.csv

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.catalogimport.domain.CsvRow
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Component
class CsvCatalogParser {

    fun parse(csvBytes: ByteArray): List<CsvRow> {
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setDelimiter(',')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build()

        try {
            InputStreamReader(ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8).use { reader ->
                CSVParser(reader, format).use { parser ->
                    val normalizedHeaders = parser.headerMap.keys.associateWith(::normalizeHeader)
                    return parser.records.map { record ->
                        val values = linkedMapOf<String, String>()
                        normalizedHeaders.forEach { (originalHeader, normalizedHeader) ->
                            val rawValue = if (record.isSet(originalHeader)) record.get(originalHeader) else ""
                            values[normalizedHeader] = rawValue.trim()
                        }
                        CsvRow(
                            rowNumber = record.recordNumber.toInt() + 1,
                            values = values,
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid CSV format: ${ex.message}", ex)
        }
    }

    private fun normalizeHeader(headerName: String): String {
        return headerName
            .trim()
            .removePrefix("\uFEFF")
            .lowercase()
    }
}
