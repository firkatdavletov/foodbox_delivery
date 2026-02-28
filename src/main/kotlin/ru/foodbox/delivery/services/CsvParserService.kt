package ru.foodbox.delivery.services

import com.opencsv.CSVReaderBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto
import ru.foodbox.delivery.services.model.UnitOfMeasure
import java.io.InputStreamReader

@Service
class CsvParserService {

    private val logger = LoggerFactory.getLogger(CsvParserService::class.java)

    fun parseCsvProducts(file: MultipartFile): ProductsImportResult {

        require(!file.isEmpty) { "File is empty" }

        val products = mutableListOf<ProductDto>()
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
                            val dto = validateAndMapProduct(row!!, rowNumber)
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

        return ProductsImportResult(products, errors)
    }

    fun parseCsvCategories(file: MultipartFile): CategoriesImportResult {

        require(!file.isEmpty) { "File is empty" }

        val categories = mutableListOf<CategoryDto>()
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
                            val dto = validateAndMapCategory(row!!, rowNumber)
                            categories.add(dto)

                        } catch (ex: Exception) {
                            val errorMessage = "Row $rowNumber: ${ex.message}"
                            logger.warn(errorMessage)
                            errors.add(errorMessage)
                        }
                    }
                }
        }

        logger.info("Import finished. Success: ${categories.size}, Errors: ${errors.size}")

        return CategoriesImportResult(categories, errors)
    }

    private fun validateAndMapProduct(row: Array<String>, rowNumber: Int): ProductDto {

        if (row.size < 8) {
            throw IllegalArgumentException("Not enough columns in $rowNumber")
        }

        val name = row[0].trim()
        if (name.isBlank()) {
            throw IllegalArgumentException("Name is blank in $rowNumber")
        }

        val description = row[1].trim()
        if (description.isBlank()) {
            throw IllegalArgumentException("Desc is blank in $rowNumber")
        }

        val price = row[2].trim().toLongOrNull()
            ?: throw IllegalArgumentException("Invalid price format: '${row[1]}' in $rowNumber")

        if (price <= 0) {
            throw IllegalArgumentException("Price must be positive in $rowNumber")
        }

        val imageUrl = row[3].trim()

        val categoryId = row[4].trim().toLongOrNull()
            ?: throw IllegalArgumentException("Category is blank in $rowNumber")

        val unit = row[5].trim().let { try {
            UnitOfMeasure.valueOf(it)
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid unit of measure: $rowNumber")
        } }

        val countOfSteps = row[6].trim().toIntOrNull()
            ?: throw IllegalArgumentException("Category is blank in $rowNumber")

        val displayWeight = row[7].trim()
        val sku = row.getOrNull(8)?.trim()?.ifBlank { null }

        return ProductDto(
            id = 0,
            categoryId = categoryId,
            title = name,
            description = description,
            price = price,
            imageUrl = imageUrl,
            unit = unit,
            countStep = countOfSteps,
            displayWeight = displayWeight,
            sku = sku,
        )
    }

    private fun validateAndMapCategory(row: Array<String>, rowNumber: Int): CategoryDto {

        if (row.size < 2) {
            throw IllegalArgumentException("Not enough columns in $rowNumber")
        }

        val name = row[0].trim()
        if (name.isBlank()) {
            throw IllegalArgumentException("Name is blank in $rowNumber")
        }

        val imageUrl = row[1].trim()
        val sku = row.getOrNull(2)?.trim()?.ifBlank { null }

        return CategoryDto(
            id = 0,
            parentCategory = null,
            title = name,
            imageUrl = imageUrl,
            sku = sku,
        )
    }
}

data class ProductsImportResult(
    val products: List<ProductDto>,
    val errors: List<String>
)

data class CategoriesImportResult(
    val categories: List<CategoryDto>,
    val errors: List<String>
)
