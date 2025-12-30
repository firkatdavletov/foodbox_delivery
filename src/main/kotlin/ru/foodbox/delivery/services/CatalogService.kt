package ru.foodbox.delivery.services

import org.springframework.cglib.core.Local
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.catalog.CatalogController
import ru.foodbox.delivery.controllers.catalog.body.DeleteCategoryResponseBody
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.data.repository.CategoryRepository
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto
import ru.foodbox.delivery.services.mapper.CategoryMapper
import ru.foodbox.delivery.services.mapper.ProductMapper
import java.time.LocalDateTime
import java.util.Date
import kotlin.jvm.optionals.getOrNull

@Service
class CatalogService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryMapper: CategoryMapper,
    private val productMapper: ProductMapper,
) {

    fun getCategories(): List<CategoryDto> {
        val entities = categoryRepository.findAll()
        return categoryMapper.toDto(entities)
    }

    fun getCategory(categoryId: Long): CategoryDto? {
        val category = categoryRepository.findById(categoryId).getOrNull() ?: return null
        return categoryMapper.toDto(category, false)
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
            parentCategoryId = categoryDto.parentCategory,
            products = mutableListOf()
        )
        val savedEntity = categoryRepository.save(categoryEntity)
        return categoryMapper.toDto(savedEntity)
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
            price = productDto.price,
            imageUrl = productDto.imageUrl,
            category = category
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
        foundProduct.price = productDto.price
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