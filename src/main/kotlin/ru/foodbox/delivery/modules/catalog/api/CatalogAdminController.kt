package ru.foodbox.delivery.modules.catalog.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.catalog.api.dto.CategoryResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductResponse
import ru.foodbox.delivery.modules.catalog.api.dto.UpsertCategoryRequest
import ru.foodbox.delivery.modules.catalog.api.dto.UpsertProductRequest
import ru.foodbox.delivery.modules.catalog.api.dto.UpsertProductVariantOptionRequest
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductOptionGroupCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductOptionValueCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantOptionCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertCategoryCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductCommand

@RestController
@RequestMapping("/api/v1/admin/catalog")
class CatalogAdminController(
    private val catalogService: CatalogService,
) {

    @GetMapping("/categories")
    fun getCategories(
        @RequestParam(name = "isActive") isActive: Boolean,
    ): List<CategoryResponse> {
        return catalogService.getAdminCategories(isActive).map { it.toResponse() }
    }

    @GetMapping("/products")
    fun getProducts(
        @RequestParam(name = "isActive") isActive: Boolean,
    ): List<ProductResponse> {
        return catalogService.getAdminProducts(isActive).map { it.toResponse() }
    }

    @PostMapping("/categories")
    fun upsertCategory(
        @Valid @RequestBody request: UpsertCategoryRequest,
    ): CategoryResponse {
        val category = catalogService.upsertCategory(
            UpsertCategoryCommand(
                id = request.id,
                name = request.name,
                slug = request.slug,
                imageIds = request.imageIds,
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
                imageIds = request.imageIds,
                unit = request.unit,
                countStep = request.countStep,
                isActive = request.isActive,
                optionGroups = request.optionGroups.map { optionGroup ->
                    ReplaceProductOptionGroupCommand(
                        code = optionGroup.code,
                        title = optionGroup.title,
                        sortOrder = optionGroup.sortOrder,
                        values = optionGroup.values.map { value ->
                            ReplaceProductOptionValueCommand(
                                code = value.code,
                                title = value.title,
                                sortOrder = value.sortOrder,
                            )
                        },
                    )
                },
                variants = request.variants.map { variant ->
                    ReplaceProductVariantCommand(
                        externalId = variant.externalId,
                        sku = variant.sku,
                        title = variant.title,
                        priceMinor = variant.priceMinor,
                        oldPriceMinor = variant.oldPriceMinor,
                        imageIds = variant.imageIds,
                        sortOrder = variant.sortOrder,
                        isActive = variant.isActive,
                        options = variant.options.map(UpsertProductVariantOptionRequest::toCommand),
                    )
                },
            )
        )

        return product.toResponse()
    }
}

private fun UpsertProductVariantOptionRequest.toCommand(): ReplaceProductVariantOptionCommand {
    return ReplaceProductVariantOptionCommand(
        optionGroupCode = optionGroupCode,
        optionValueCode = optionValueCode,
    )
}
