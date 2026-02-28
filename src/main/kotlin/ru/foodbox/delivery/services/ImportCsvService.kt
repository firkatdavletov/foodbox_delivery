package ru.foodbox.delivery.services

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import ru.foodbox.delivery.controllers.admin.body.ImportFileResponseBody
import ru.foodbox.delivery.data.repository.CategoryRepository
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto
import ru.foodbox.delivery.services.mapper.CategoryMapper
import ru.foodbox.delivery.services.mapper.ProductMapper
import java.math.BigDecimal

@Service
class ImportCsvService(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val csvParserService: CsvParserService,
    private val productMapper: ProductMapper,
    private val categoryMapper: CategoryMapper,
) {
    fun importCsvProducts(file: MultipartFile, importMode: String): ImportFileResponseBody {
        val importAction = when (importMode) {
            "insert" -> ::insertProducts
            "update" -> ::updateProducts
            else -> return ImportFileResponseBody(false, "Неизвестный режим импорта: $importMode", 400)
        }

        val importResult = csvParserService.parseCsvProducts(file)

        if (importResult.errors.isNotEmpty()) {
            val errors = importResult.errors.joinToString("\n")
            return ImportFileResponseBody(false, errors, 400)
        }

        if (importResult.products.isEmpty()) {
            return ImportFileResponseBody(false, "Список продуктов пустой", 400)
        }

        val error = importAction(importResult.products)

        return if (error == null) {
            ImportFileResponseBody(true, null, 200)
        } else {
            ImportFileResponseBody(false, error, 400)
        }
    }

    fun importCsvCategories(file: MultipartFile, importMode: String): ImportFileResponseBody {
        val importAction = when (importMode) {
            "insert" -> ::insertCategories
            "update" -> ::updateCategories
            else -> return ImportFileResponseBody(false, "Неизвестный режим импорта: $importMode", 400)
        }

        val importResult = csvParserService.parseCsvCategories(file)

        if (importResult.errors.isNotEmpty()) {
            val errors = importResult.errors.joinToString("\n")
            return ImportFileResponseBody(false, errors, 400)
        }

        if (importResult.categories.isEmpty()) {
            return ImportFileResponseBody(false, "Список категорий пустой", 400)
        }

        val error = importAction(importResult.categories)

        return if (error == null) {
            ImportFileResponseBody(true, null, 200)
        } else {
            ImportFileResponseBody(false, error, 400)
        }
    }

    @Transactional
    private fun insertProducts(dto: List<ProductDto>): String? {
        // 1. Деактивируем все продукты
        productRepository.markAllProductsAsInactive()

        val entities = dto.map { productDto ->
            val category = categoryRepository.findById(productDto.categoryId).orElse(null)
                ?: return "Не найдена категория ${productDto.categoryId} продукта ${productDto.title}"

            productMapper.toEntity(productDto, category)
        }

        // 3. Создание продуктов
        productRepository.saveAll(entities)
        return null
    }

    @Transactional
    private fun updateProducts(dto: List<ProductDto>): String? {
        val skus = mutableListOf<String>()
        for (productDto in dto) {
            val sku = productDto.sku?.trim().orEmpty()
            if (sku.isBlank()) {
                return "У продукта ${productDto.title} не указан sku"
            }
            skus.add(sku)
        }

        val duplicateSku = skus
            .groupingBy { it }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key
        if (duplicateSku != null) {
            return "Дублирующийся sku в файле: $duplicateSku"
        }

        val productsBySku = productRepository.findAllBySkuIn(skus)
            .associateBy { it.sku!! }

        val missingProductSku = skus.firstOrNull { it !in productsBySku }
        if (missingProductSku != null) {
            return "Не найден продукт с sku $missingProductSku"
        }

        val categoryIds = dto.map { it.categoryId }.distinct()
        val categoriesById = categoryRepository.findAllById(categoryIds)
            .associateBy { it.id!! }

        val missingCategoryId = categoryIds.firstOrNull { it !in categoriesById }
        if (missingCategoryId != null) {
            return "Не найдена категория $missingCategoryId"
        }

        val updatedProducts = dto.map { productDto ->
            val sku = productDto.sku!!.trim()
            val productEntity = productsBySku.getValue(sku)
            val category = categoriesById.getValue(productDto.categoryId)

            productEntity.apply {
                title = productDto.title
                description = productDto.description
                price = productDto.price.toBigDecimal() / BigDecimal(100)
                imageUrl = productDto.imageUrl
                unit = productDto.unit
                countStep = productDto.countStep
                displayWeight = productDto.displayWeight
                this.category = category
                isActive = true
                this.sku = sku
            }
        }

        productRepository.saveAll(updatedProducts)
        return null
    }

    @Transactional
    private fun insertCategories(dto: List<CategoryDto>): String? {
        categoryRepository.markAllCategoriesAsInactive()
        val entities = dto.map { categoryDto ->
            categoryMapper.toEntity(categoryDto)
        }
        categoryRepository.saveAll(entities)
        return null
    }

    @Transactional
    private fun updateCategories(dto: List<CategoryDto>): String? {
        val skus = mutableListOf<String>()
        for (categoryDto in dto) {
            val sku = categoryDto.sku?.trim().orEmpty()
            if (sku.isBlank()) {
                return "У категории ${categoryDto.title} не указан sku"
            }
            skus.add(sku)
        }

        val duplicateSku = skus
            .groupingBy { it }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key
        if (duplicateSku != null) {
            return "Дублирующийся sku в файле: $duplicateSku"
        }

        val categoriesBySku = categoryRepository.findAllBySkuIn(skus)
            .associateBy { it.sku!! }

        val missingCategorySku = skus.firstOrNull { it !in categoriesBySku }
        if (missingCategorySku != null) {
            return "Не найдена категория с sku $missingCategorySku"
        }

        val updatedCategories = dto.map { categoryDto ->
            val sku = categoryDto.sku!!.trim()
            val categoryEntity = categoriesBySku.getValue(sku)

            categoryEntity.apply {
                title = categoryDto.title
                imageUrl = categoryDto.imageUrl
                isActive = true
                this.sku = sku
            }
        }

        categoryRepository.saveAll(updatedCategories)
        return null
    }
}
