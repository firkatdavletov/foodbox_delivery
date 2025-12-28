package ru.foodbox.delivery.controllers.catalog

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.controllers.catalog.body.CreateCategoryRequestBody
import ru.foodbox.delivery.controllers.catalog.body.CreateCategoryResponseBody
import ru.foodbox.delivery.controllers.catalog.body.CreateProductRequestBody
import ru.foodbox.delivery.controllers.catalog.body.CreateProductResponseBody
import ru.foodbox.delivery.controllers.catalog.body.DeleteCategoryResponseBody
import ru.foodbox.delivery.controllers.catalog.body.GetCatalogResponseBody
import ru.foodbox.delivery.controllers.catalog.body.GetCategoriesResponseBody
import ru.foodbox.delivery.controllers.catalog.body.GetCategoryResponseBody
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.services.CatalogService
import ru.foodbox.delivery.controllers.catalog.body.GetProductsResponseBody
import java.math.BigDecimal

@RestController
@RequestMapping("/catalog")
class CatalogController(
    private val catalogService: CatalogService
) {
    @GetMapping("/categories")
    fun getCategories(): ResponseEntity<GetCategoriesResponseBody> {
        val categories = catalogService.getCategories()
        val response = GetCategoriesResponseBody(categories)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/products")
    fun getProducts(@RequestParam categoryId: Long): ResponseEntity<GetProductsResponseBody> {
        val category = catalogService.getCategory(categoryId)
            ?: return ResponseEntity.ok(GetProductsResponseBody(null, emptyList(), false,"Категория не найдена", 404))
        val products = catalogService.getProducts(category.id)

        return ResponseEntity.ok(GetProductsResponseBody(
            category = category,
            products = products,
            true,
            null,
            null,
        ))
    }

    @PostMapping("/products")
    fun createProduct(@RequestBody body: CreateProductRequestBody): ResponseEntity<CreateProductResponseBody> {
        val savedProduct = catalogService.insertProduct(body.product)
        return ResponseEntity.ok(CreateProductResponseBody(savedProduct))
    }

    @PostMapping("/categories")
    fun createCategory(@RequestBody body: CreateCategoryRequestBody): ResponseEntity<CreateCategoryResponseBody> {
        val savedCategory = catalogService.insertCategory(body.category)
        return ResponseEntity.ok(CreateCategoryResponseBody(savedCategory))
    }

    @DeleteMapping("category")
    fun deleteCategory(@RequestParam categoryId: Long): ResponseEntity<DeleteCategoryResponseBody> {
        val result = catalogService.deleteCategory(categoryId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("category")
    fun getCategory(@RequestParam id: Long): ResponseEntity<GetCategoryResponseBody> {
        val result = catalogService.getCategory(id)

        return if (result == null) {
            ResponseEntity.ok(GetCategoryResponseBody("Категория не найдена", 404))
        } else {
            ResponseEntity.ok(GetCategoryResponseBody(result))
        }
    }
}