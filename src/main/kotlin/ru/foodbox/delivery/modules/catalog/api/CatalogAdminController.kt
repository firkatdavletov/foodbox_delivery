package ru.foodbox.delivery.modules.catalog.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.catalog.api.dto.CategoryResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductResponse
import ru.foodbox.delivery.modules.catalog.api.dto.UpsertCategoryRequest
import ru.foodbox.delivery.modules.catalog.api.dto.UpsertProductRequest
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import ru.foodbox.delivery.modules.catalog.application.command.UpsertCategoryCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductCommand

@RestController
@RequestMapping("/api/v1/admin/catalog")
class CatalogAdminController(
    private val catalogService: CatalogService,
) {

    @PostMapping("/categories")
    fun upsertCategory(
        @Valid @RequestBody request: UpsertCategoryRequest,
    ): CategoryResponse {
        val category = catalogService.upsertCategory(
            UpsertCategoryCommand(
                id = request.id,
                name = request.name,
                slug = request.slug,
                imageUrl = request.imageUrl,
                isActive = request.isActive,
            )
        )

        return category.toResponse()
    }

    @PostMapping("/products")
    fun upsertProduct(
        @Valid @RequestBody request: UpsertProductRequest,
    ): ProductResponse {
        val product = catalogService.upsertProduct(
            UpsertProductCommand(
                id = request.id,
                categoryId = request.categoryId,
                title = request.title,
                slug = request.slug,
                description = request.description,
                priceMinor = request.priceMinor,
                oldPriceMinor = request.oldPriceMinor,
                sku = request.sku,
                imageUrl = request.imageUrl,
                unit = request.unit,
                countStep = request.countStep,
                isActive = request.isActive,
            )
        )

        return product.toResponse()
    }
}
