package ru.foodbox.delivery.modules.catalog.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
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
import ru.foodbox.delivery.modules.catalog.modifier.application.command.ReplaceProductModifierGroupCommand
import java.util.UUID

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

    @DeleteMapping("/categories/{categoryId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategoryImage(
        @PathVariable categoryId: UUID,
        @PathVariable imageId: UUID,
    ) {
        catalogService.deleteCategoryImage(categoryId, imageId)
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
                modifierGroups = request.modifierGroups.map { modifierGroup ->
                    ReplaceProductModifierGroupCommand(
                        modifierGroupId = modifierGroup.modifierGroupId,
                        sortOrder = modifierGroup.sortOrder,
                        isActive = modifierGroup.isActive,
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

    @DeleteMapping("/products/{productId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProductImage(
        @PathVariable productId: UUID,
        @PathVariable imageId: UUID,
    ) {
        catalogService.deleteProductImage(productId, imageId)
    }
}

private fun UpsertProductVariantOptionRequest.toCommand(): ReplaceProductVariantOptionCommand {
    return ReplaceProductVariantOptionCommand(
        optionGroupCode = optionGroupCode,
        optionValueCode = optionValueCode,
    )
}
