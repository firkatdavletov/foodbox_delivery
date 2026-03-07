package ru.foodbox.delivery.services

import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.admin.body.SaveCategoryResponseBody
import ru.foodbox.delivery.controllers.admin.body.SaveProductResponseBody
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.data.repository.CartItemRepository
import ru.foodbox.delivery.data.repository.CategoryRepository
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto
import ru.foodbox.delivery.services.mapper.CategoryMapper
import ru.foodbox.delivery.services.mapper.ProductMapper
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
    companion object {
        private const val NEW_PRODUCTS_LIMIT = 8
        private const val NEW_PRODUCTS_DAYS = 30L
        private const val SEARCH_PRODUCTS_LIMIT = 8
    }

    fun getCategories(): List<CategoryDto> {
        val entities = categoryRepository.findAll()
        return categoryMapper.toDto(entities)
    }

    fun getCategory(categoryId: Long): CategoryDto? {
        val category = categoryRepository.findById(categoryId).getOrNull() ?: return null
        return categoryMapper.toDto(category)
    }

    fun getProducts(categoryId: Long): List<ProductDto> {
        val products = productRepository.findAllByCategoryIdAndIsActiveTrue(categoryId)
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

    fun getNewProducts(): List<ProductDto> {
        val createdFrom = LocalDateTime.now().minusDays(NEW_PRODUCTS_DAYS)
        val pageable = PageRequest.of(0, NEW_PRODUCTS_LIMIT)
        val products = productRepository
            .findAllByIsActiveTrueAndShowInCollectionsTrueAndCreatedGreaterThanEqualOrderByCreatedDesc(
                createdFrom = createdFrom,
                pageable = pageable,
            )

        return productMapper.toDto(products)
    }

    fun searchProducts(query: String): List<ProductDto> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }

        val pageable = PageRequest.of(0, SEARCH_PRODUCTS_LIMIT)
        val products = productRepository.findAllByIsActiveTrueAndTitleContainingIgnoreCaseOrderByTitleAsc(
            title = normalizedQuery,
            pageable = pageable,
        )
        return productMapper.toDto(products)
    }

    fun insertCategory(categoryDto: CategoryDto): CategoryDto {
        val categoryEntity = CategoryEntity(
            title = categoryDto.title,
            imageUrl = categoryDto.imageUrl,
            products = mutableListOf(),
            sku = categoryDto.sku,
        )
        val savedEntity = categoryRepository.save(categoryEntity)
        return categoryMapper.toDto(savedEntity)
    }

    fun updateCategory(categoryDto: CategoryDto): CategoryDto? {
        val foundCategory = categoryRepository.findById(categoryDto.id).getOrNull() ?: return null
        foundCategory.title = categoryDto.title
        foundCategory.imageUrl = categoryDto.imageUrl
        foundCategory.sku = categoryDto.sku

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
            category = category,
            unit = productDto.unit,
            countStep = productDto.countStep,
            displayWeight = productDto.displayWeight,
            sku = productDto.sku,
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
        foundProduct.category = category
        foundProduct.description = productDto.description
        foundProduct.price = BigDecimal(productDto.price / 100)
        foundProduct.sku = productDto.sku
        foundProduct.modified = LocalDateTime.now()
        val savedProduct = productRepository.save(foundProduct)
        return productMapper.toDto(savedProduct)
    }

    fun deleteCategory(categoryId: Long): Boolean {
        categoryRepository.makeCategoryAsInactive(categoryId)
        return true
    }

    fun deleteProduct(id: Long): Boolean {
        productRepository.makeProductAsInactive(id)
        return true
    }

    fun saveCategory(categoryDto: CategoryDto): SaveCategoryResponseBody {
        val categoryEntity = categoryRepository.findById(categoryDto.id).getOrNull()

        val savedCategory = if (categoryEntity != null) {
            categoryEntity.title = categoryDto.title
            categoryEntity.imageUrl = categoryDto.imageUrl
            categoryEntity.sku = categoryDto.sku
            categoryRepository.save(categoryEntity)
        } else {
            categoryRepository.save(categoryMapper.toEntity(categoryDto))
        }

        return SaveCategoryResponseBody(categoryMapper.toDto(savedCategory))
    }

    fun saveProduct(productDto: ProductDto): SaveProductResponseBody {
        val productEntity = productRepository.findById(productDto.id).getOrNull()
        val category = categoryRepository.findById(productDto.categoryId).getOrNull()
            ?: return SaveProductResponseBody("Категория не найдена", 404)

        val savedProduct = if (productEntity != null) {
            productEntity.title = productDto.title
            productEntity.sku = productDto.sku
            productEntity.unit = productDto.unit
            productEntity.countStep = productDto.countStep
            productEntity.displayWeight = productDto.displayWeight
            productEntity.sku = productDto.sku
            productEntity.modified = LocalDateTime.now()
            productEntity.category = category
            productEntity.description = productDto.description
            productRepository.save(productEntity)
        } else {
            productRepository.save(productMapper.toEntity(productDto, category))
        }

        return SaveProductResponseBody(productMapper.toDto(savedProduct))
    }
}
