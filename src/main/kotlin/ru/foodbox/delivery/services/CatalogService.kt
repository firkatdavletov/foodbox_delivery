package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.catalog.CatalogController
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.data.repository.CategoryRepository
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto
import ru.foodbox.delivery.services.mapper.CategoryMapper
import ru.foodbox.delivery.services.mapper.ProductMapper

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

    fun getCategory(categoryId: Long): CategoryDto {
        val category = categoryRepository.findById(categoryId).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(404), "Категория не найдена")
        }
        return categoryMapper.toDto(category)
    }

    fun getProducts(categoryId: Long): List<ProductDto> {
        val products = productRepository.findAllByCategoryId(categoryId)
        return productMapper.toDto(products)
    }

    fun getAllProducts(): List<ProductDto> {
        val products = productRepository.findAll()
        return productMapper.toDto(products)
    }
}