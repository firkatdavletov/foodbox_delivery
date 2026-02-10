package ru.foodbox.delivery.services

import com.opencsv.CSVReaderBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import ru.foodbox.delivery.services.dto.ProductCsvDto
import java.io.InputStreamReader
import java.math.BigDecimal

@Service
class CsvParserService {

    private val logger = LoggerFactory.getLogger(CsvParserService::class.java)

    fun parseCsv(file: MultipartFile): ImportResult {

        require(!file.isEmpty) { "File is empty" }

        val products = mutableListOf<ProductCsvDto>()
        val errors = mutableListOf<String>()

        InputStreamReader(file.inputStream).use { isr ->
            CSVReaderBuilder(isr)
                .withSkipLines(1) // пропускаем header
                .build()
                .use { reader ->

                    var rowNumber = 1 // т.к. header = 1

                    var row: Array<String>?
                    while (reader.readNext().also { row = it } != null) {
                        rowNumber++

                        try {
                            val dto = validateAndMap(row!!, rowNumber)
                            products.add(dto)

                        } catch (ex: Exception) {
                            val errorMessage = "Row $rowNumber: ${ex.message}"
                            logger.warn(errorMessage)
                            errors.add(errorMessage)
                        }
                    }
                }
        }

        logger.info("Import finished. Success: ${products.size}, Errors: ${errors.size}")

        return ImportResult(products, errors)
    }

    private fun validateAndMap(row: Array<String>, rowNumber: Int): ProductCsvDto {

        if (row.size < 3) {
            throw IllegalArgumentException("Not enough columns")
        }

        val name = row[0].trim()
        if (name.isBlank()) {
            throw IllegalArgumentException("Name is blank")
        }

        val description = row[1].trim()
        if (name.isBlank()) {
            throw IllegalArgumentException("Desc is blank")
        }

        val price = row[2].trim().toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Invalid price format: '${row[1]}'")

        if (price < BigDecimal.ZERO) {
            throw IllegalArgumentException("Price must be positive")
        }

        val imageUrl = row[3].trim()
        if (name.isBlank()) {
            throw IllegalArgumentException("Name is blank")
        }

        val category = row[4].trim()
        if (category.isBlank()) {
            throw IllegalArgumentException("Category is blank")
        }

        return ProductCsvDto(
            name = name,
            description = description,
            price = price,
            imageUrl = imageUrl,
            category = category
        )
    }
}

data class ImportResult(
    val products: List<ProductCsvDto>,
    val errors: List<String>
)