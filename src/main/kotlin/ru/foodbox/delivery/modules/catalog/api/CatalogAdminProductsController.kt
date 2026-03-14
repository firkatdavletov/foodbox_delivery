package ru.foodbox.delivery.modules.catalog.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.api.dto.ProductDetailsResponse
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/products")
class CatalogAdminProductsController(
    private val catalogService: CatalogService,
) {

    @GetMapping("/{productId}")
    fun getProductDetails(
        @PathVariable productId: UUID,
    ): ProductDetailsResponse {
        val product = catalogService.getAdminProductDetails(productId)
            ?: throw NotFoundException("Product not found")

        return product.toDetailsResponse()
    }
}
