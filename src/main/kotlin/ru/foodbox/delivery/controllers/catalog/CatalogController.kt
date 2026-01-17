package ru.foodbox.delivery.controllers.catalog

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.foodbox.delivery.controllers.catalog.body.*
import ru.foodbox.delivery.controllers.catalog.body.DeleteProductResponseBody
import ru.foodbox.delivery.services.CatalogService

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

    @PostMapping("/product/new")
    fun createProduct(@RequestBody body: CreateProductRequestBody): ResponseEntity<CreateProductResponseBody> {
        val savedProduct = catalogService.insertProduct(body.product)
        return ResponseEntity.ok(CreateProductResponseBody(savedProduct))
    }

    @PostMapping("/product/update")
    fun updateProduct(@RequestBody body: CreateProductRequestBody): ResponseEntity<CreateProductResponseBody> {
        val savedProduct = catalogService.updateProduct(body.product)
        return if (savedProduct != null) {
            ResponseEntity.ok(CreateProductResponseBody(savedProduct))
        } else {
            ResponseEntity.ok(CreateProductResponseBody("Ошибка создания номенклатуры", 200))
        }
    }

    @DeleteMapping("/product")
    fun deleteProduct(@RequestParam id: Long): DeleteProductResponseBody {
        val result = catalogService.deleteProduct(id)
        return DeleteProductResponseBody()
    }

    @PostMapping("/category/new")
    fun createCategory(@RequestBody body: CreateCategoryRequestBody): ResponseEntity<CreateCategoryResponseBody> {
        val savedCategory = catalogService.insertCategory(body.category)
        return ResponseEntity.ok(CreateCategoryResponseBody(savedCategory))
    }

    @PostMapping("/category/update")
    fun updateCategory(@RequestBody body: CreateCategoryRequestBody): ResponseEntity<CreateCategoryResponseBody> {
        val savedCategory = catalogService.updateCategory(body.category)

        return if (savedCategory != null) {
            ResponseEntity.ok(CreateCategoryResponseBody(savedCategory))
        } else {
            ResponseEntity.ok(CreateCategoryResponseBody("Категория не найдена", 404))
        }
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