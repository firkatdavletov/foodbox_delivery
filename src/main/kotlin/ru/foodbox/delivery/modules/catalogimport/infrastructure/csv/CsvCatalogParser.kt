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
        try {
            val commaParsed = parseWithDelimiter(csvBytes, ',')
            if (!shouldRetryWithSemicolon(commaParsed.originalHeaders)) {
                return commaParsed.rows
            }

            return runCatching { parseWithDelimiter(csvBytes, ';').rows }
                .getOrElse { commaParsed.rows }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid CSV format: ${ex.message}", ex)
        }
    }

    private fun parseWithDelimiter(csvBytes: ByteArray, delimiter: Char): ParsedCsv {
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setDelimiter(delimiter)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build()

        InputStreamReader(ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8).use { reader ->
            CSVParser(reader, format).use { parser ->
                val originalHeaders = parser.headerMap.keys.toList()
                val normalizedHeaders = originalHeaders.associateWith(::normalizeHeader)
                val rows = parser.records.map { record ->
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

                return ParsedCsv(
                    originalHeaders = originalHeaders,
                    rows = rows,
                )
            }
        }
    }

    private fun shouldRetryWithSemicolon(originalHeaders: List<String>): Boolean {
        if (originalHeaders.size != 1) {
            return false
        }
        return originalHeaders.first().contains(';')
    }

    private fun normalizeHeader(headerName: String): String {
        val normalized = headerName
            .trim()
            .removePrefix("\uFEFF")
            .replace('\u00A0', ' ')
            .lowercase()

        val canonical = normalized
            .replace(REQUIREMENT_MARK_REGEX, "")
            .replace(MULTIPLE_SPACES_REGEX, " ")
            .trim()

        HEADER_ALIASES[canonical]?.let { return it }

        OPTION_GROUP_CODE_ALIAS_REGEX.matchEntire(canonical)?.groupValues?.get(1)?.let { position ->
            return "option${position}_group_code"
        }
        OPTION_GROUP_TITLE_ALIAS_REGEX.matchEntire(canonical)?.groupValues?.get(1)?.let { position ->
            return "option${position}_group_title"
        }
        OPTION_VALUE_CODE_ALIAS_REGEX.matchEntire(canonical)?.groupValues?.get(1)?.let { position ->
            return "option${position}_value_code"
        }
        OPTION_VALUE_TITLE_ALIAS_REGEX.matchEntire(canonical)?.groupValues?.get(1)?.let { position ->
            return "option${position}_value_title"
        }

        return canonical
    }

    private companion object {
        val REQUIREMENT_MARK_REGEX = Regex("\\s*\\((обязательное|необязательное)[^)]*\\)\\s*$")
        val MULTIPLE_SPACES_REGEX = Regex("\\s+")
        val OPTION_GROUP_CODE_ALIAS_REGEX = Regex("^код группы опции\\s*(\\d+)$")
        val OPTION_GROUP_TITLE_ALIAS_REGEX = Regex("^название группы опции\\s*(\\d+)$")
        val OPTION_VALUE_CODE_ALIAS_REGEX = Regex("^код значения опции\\s*(\\d+)$")
        val OPTION_VALUE_TITLE_ALIAS_REGEX = Regex("^название значения опции\\s*(\\d+)$")

        val HEADER_ALIASES = mapOf(
            "внешний id товара" to "product_external_id",
            "слаг товара" to "product_slug",
            "название товара" to "product_title",
            "внешний id категории" to "category_external_id",
            "описание товара" to "product_description",
            "бренд" to "product_brand",
            "ссылка на изображение товара" to "product_image_url",
            "цена товара в копейках" to "product_price_minor",
            "старая цена товара в копейках" to "product_old_price_minor",
            "единица измерения" to "product_unit",
            "шаг количества" to "product_count_step",
            "товар активен" to "product_is_active",
            "порядок сортировки товара" to "product_sort_order",
            "внешний id варианта" to "variant_external_id",
            "sku варианта" to "variant_sku",
            "название варианта" to "variant_title",
            "цена варианта в копейках" to "variant_price_minor",
            "старая цена варианта в копейках" to "variant_old_price_minor",
            "ссылка на изображение варианта" to "variant_image_url",
            "порядок сортировки варианта" to "variant_sort_order",
            "вариант активен" to "variant_is_active",
            "внешний id категории в каталоге" to "external_id",
            "название категории" to "name",
            "слаг категории" to "slug",
            "внешний id родительской категории" to "parent_external_id",
            "описание категории" to "description",
            "категория активна" to "is_active",
            "порядок сортировки категории" to "sort_order",
        )
    }

    private data class ParsedCsv(
        val originalHeaders: List<String>,
        val rows: List<CsvRow>,
    )
}
