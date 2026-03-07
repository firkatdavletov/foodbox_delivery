package ru.foodbox.delivery.controllers.catalog

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.foodbox.delivery.controllers.catalog.body.*
import ru.foodbox.delivery.services.CatalogService

@RestController
@RequestMapping(value = ["/catalog", "/api/catalog"])
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
            ?: return ResponseEntity.ok(GetProductsResponseBody(emptyList(), false,"Категория не найдена", 404))
        val products = catalogService.getProducts(category.id)

        return ResponseEntity.ok(GetProductsResponseBody(
            products = products,
            true,
            null,
            null,
        ))
    }

    @GetMapping("/product")
    fun getProduct(@RequestParam id: Long): ResponseEntity<GetProductResponseBody> {
        val product = catalogService.getProduct(id)
        return if (product != null) {
            ResponseEntity.ok(GetProductResponseBody(product))
        } else {
            ResponseEntity.ok(GetProductResponseBody("Продукт не найден", 404))
        }
    }

    @GetMapping("/products/all")
    fun getAllProducts(): ResponseEntity<GetProductsResponseBody> {
        val products = catalogService.getAllProducts()

        return ResponseEntity.ok(GetProductsResponseBody(products))
    }

    @GetMapping("/products/new")
    fun getNewProducts(): ResponseEntity<GetProductsResponseBody> {
        val products = catalogService.getNewProducts()
        return ResponseEntity.ok(GetProductsResponseBody(products))
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
