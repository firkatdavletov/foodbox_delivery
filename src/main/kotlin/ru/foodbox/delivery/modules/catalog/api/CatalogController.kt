package ru.foodbox.delivery.modules.catalog.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.api.dto.CategoryResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductDetailsResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductResponse
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController(
    private val catalogService: CatalogService,
) {

    @GetMapping("/categories")
    fun getCategories(
        @RequestParam(name = "activeOnly", defaultValue = "true") activeOnly: Boolean,
        @RequestParam(name = "limit", defaultValue = "100") limit: Int,
    ): List<CategoryResponse> {
        return catalogService.getCategories(activeOnly, limit).map { it.toResponse() }
    }

    @GetMapping("/products")
    fun getProducts(
        @RequestParam(name = "categoryId", required = false) categoryId: UUID?,
        @RequestParam(name = "query", required = false) query: String?,
    ): List<ProductResponse> {
        return catalogService.getProducts(categoryId = categoryId, query = query)
            .map { it.toResponse() }
    }

    @GetMapping("/products/popular")
    fun getPopularProducts(
        @RequestParam(name = "limit", defaultValue = "20") limit: Int,
    ): List<ProductResponse> {
        return catalogService.getPopularProducts(limit = limit)
            .map { it.toResponse() }
    }

    @GetMapping("/products/{productId}")
    fun getProduct(
        @PathVariable productId: UUID,
    ): ProductDetailsResponse {
        val product = catalogService.getProductDetails(productId)
            ?: throw NotFoundException("Product not found")

        return product.toPublicDetailsResponse()
    }
}
