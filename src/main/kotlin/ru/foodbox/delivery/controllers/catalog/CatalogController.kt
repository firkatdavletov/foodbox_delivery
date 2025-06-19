package ru.foodbox.delivery.controllers.catalog

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.controllers.catalog.body.GetCatalogResponseBody
import ru.foodbox.delivery.controllers.catalog.body.GetCategoriesResponseBody
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
    fun getProducts(@RequestParam categoryId: Long): GetProductsResponseBody {
        val category = catalogService.getCategory(categoryId)
        val products = catalogService.getProducts(category.id)

        return GetProductsResponseBody(
            category = category,
            products = products
        )
    }

    @GetMapping
    fun getCatalog(): GetCatalogResponseBody {
        val categories = catalogService.getCategories()
        val products = catalogService.getAllProducts()
        return GetCatalogResponseBody(
            categories = categories,
            products = products
        )
    }
}