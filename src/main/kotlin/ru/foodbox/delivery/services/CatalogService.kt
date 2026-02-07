package ru.foodbox.delivery.services

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.catalog.body.DeleteCategoryResponseBody
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.data.repository.CartItemRepository
import ru.foodbox.delivery.data.repository.CategoryRepository
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductCsvDto
import ru.foodbox.delivery.services.dto.ProductDto
import ru.foodbox.delivery.services.mapper.CategoryMapper
import ru.foodbox.delivery.services.mapper.ProductMapper
import ru.foodbox.delivery.services.model.UnitOfMeasure
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class CatalogService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val cartItemRepository: CartItemRepository,
    private val categoryMapper: CategoryMapper,
    private val productMapper: ProductMapper,
) {

    fun getCategories(): List<CategoryDto> {
        val entities = categoryRepository.findAll()
        return categoryMapper.toDto(entities)
    }

    fun getCategory(categoryId: Long): CategoryDto? {
        val category = categoryRepository.findById(categoryId).getOrNull() ?: return null
        return categoryMapper.toDto(category)
    }

    fun getProducts(categoryId: Long): List<ProductDto> {
        val products = productRepository.findAllByCategoryId(categoryId)
        return productMapper.toDto(products)
    }

    fun getProduct(id: Long): ProductDto? {
        val entity = productRepository.findById(id).getOrNull() ?: return null
        return productMapper.toDto(entity)
    }

    fun getAllProducts(): List<ProductDto> {
        val products = productRepository.findAll()
        return productMapper.toDto(products)
    }

    fun insertCategory(categoryDto: CategoryDto): CategoryDto {
        val categoryEntity = CategoryEntity(
            title = categoryDto.title,
            imageUrl = categoryDto.imageUrl,
            products = mutableListOf()
        )
        val savedEntity = categoryRepository.save(categoryEntity)
        return categoryMapper.toDto(savedEntity)
    }
    @Transactional
    fun insertCatalogFromCsv(csvs: List<ProductCsvDto>) {
        // 1. Очистка
        cartItemRepository.deleteAllInBatch()
        productRepository.deleteAllInBatch()
        categoryRepository.deleteAllInBatch()

        // 2. Создание категорий
        val categoryMap = csvs
            .mapNotNull { it.category }
            .distinct()
            .associateWith { title ->
                CategoryEntity(title = title)
            }

        categoryRepository.saveAll(categoryMap.values)

        // 3. Создание продуктов
        val products = csvs.map { csv ->
            val category = categoryMap[csv.category]
                ?: error("Category missing")

            ProductEntity(
                title = csv.name ?: "",
                description = csv.description,
                price = csv.price ?: BigDecimal(9999.0),
                category = category,
                unit = UnitOfMeasure.PIECE,
                countStep = 1,
                displayWeight = null,
            ).apply {
                created = LocalDateTime.now()
                modified = LocalDateTime.now()
            }
        }

        productRepository.saveAll(products)
    }

    fun updateCategory(categoryDto: CategoryDto): CategoryDto? {
        val foundCategory = categoryRepository.findById(categoryDto.id).getOrNull() ?: return null
        foundCategory.title = categoryDto.title
        foundCategory.imageUrl = categoryDto.imageUrl

        val savedEntity = categoryRepository.save(foundCategory)
        return categoryMapper.toDto(savedEntity)
    }

    fun insertProduct(productDto: ProductDto): ProductDto {
        val category = categoryRepository.findById(productDto.categoryId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404))
        val newProduct = ProductEntity(
            title = productDto.title,
            description = productDto.description,
            price = BigDecimal(productDto.price / 100),
            imageUrl = productDto.imageUrl,
            category = category,
            unit = productDto.unit,
            countStep = productDto.countStep,
            displayWeight = productDto.displayWeight,
        )
        newProduct.created = LocalDateTime.now()
        newProduct.modified = LocalDateTime.now()
        val savedProduct = productRepository.save(newProduct)
        return productMapper.toDto(savedProduct)
    }

    fun updateProduct(productDto: ProductDto): ProductDto? {
        val foundProduct = productRepository.findById(productDto.id).getOrNull() ?: return null
        val category = categoryRepository.findById(productDto.categoryId).getOrNull() ?: return null
        foundProduct.title = productDto.title
        foundProduct.imageUrl = productDto.imageUrl
        foundProduct.category = category
        foundProduct.description = productDto.description
        foundProduct.price = BigDecimal(productDto.price / 100)
        foundProduct.modified = LocalDateTime.now()
        val savedProduct = productRepository.save(foundProduct)
        return productMapper.toDto(savedProduct)
    }

    fun deleteCategory(categoryId: Long): DeleteCategoryResponseBody {
        val products = productRepository.findAllByCategoryId(categoryId)
        if (products.isNotEmpty()) {
            return DeleteCategoryResponseBody(
                error = "В категории есть продукты, сначала удалите все продукты",
                100
            )
        }
        categoryRepository.deleteById(categoryId)
        return DeleteCategoryResponseBody()
    }

    fun deleteProduct(id: Long): Boolean {
        productRepository.deleteById(id)
        return true
    }
}