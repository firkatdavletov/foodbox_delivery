package ru.foodbox.delivery.modules.catalog.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.api.dto.AdminProductDetailsResponse
import ru.foodbox.delivery.modules.catalog.api.dto.AdminProductVariantResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductOptionGroupResponse
import ru.foodbox.delivery.modules.catalog.api.dto.ProductOptionValueResponse
import ru.foodbox.delivery.modules.catalog.api.dto.UpsertProductOptionGroupRequest
import ru.foodbox.delivery.modules.catalog.api.dto.UpsertProductOptionValueRequest
import ru.foodbox.delivery.modules.catalog.api.dto.UpsertProductVariantRequest
import ru.foodbox.delivery.modules.catalog.application.CatalogProductVariantsService
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductOptionGroupCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductOptionValueCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductVariantCommand
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/products")
class CatalogAdminProductsController(
    private val catalogService: CatalogService,
    private val productVariantsService: CatalogProductVariantsService,
) {

    @GetMapping("/{productId}")
    fun getProductDetails(
        @PathVariable productId: UUID,
    ): AdminProductDetailsResponse {
        val product = catalogService.getAdminProductDetails(productId)
            ?: throw NotFoundException("Product not found")

        return product.toAdminDetailsResponse()
    }

    @GetMapping("/{productId}/option-groups/{optionGroupId}")
    fun getProductOptionGroup(
        @PathVariable productId: UUID,
        @PathVariable optionGroupId: UUID,
    ): ProductOptionGroupResponse {
        return productVariantsService.getOptionGroup(productId, optionGroupId)?.toResponse()
            ?: throw NotFoundException("Option group not found")
    }

    @PostMapping("/{productId}/option-groups")
    fun upsertProductOptionGroup(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpsertProductOptionGroupRequest,
    ): ProductOptionGroupResponse {
        return productVariantsService.upsertOptionGroup(
            UpsertProductOptionGroupCommand(
                id = request.id,
                productId = productId,
                code = request.code,
                title = request.title,
                sortOrder = request.sortOrder,
            )
        ).toResponse()
    }

    @PostMapping("/{productId}/option-groups/{optionGroupId}/values")
    fun upsertProductOptionValue(
        @PathVariable productId: UUID,
        @PathVariable optionGroupId: UUID,
        @Valid @RequestBody request: UpsertProductOptionValueRequest,
    ): ProductOptionValueResponse {
        return productVariantsService.upsertOptionValue(
            UpsertProductOptionValueCommand(
                id = request.id,
                productId = productId,
                optionGroupId = optionGroupId,
                code = request.code,
                title = request.title,
                sortOrder = request.sortOrder,
            )
        ).toResponse()
    }

    @GetMapping("/{productId}/variants/{variantId}")
    fun getProductVariant(
        @PathVariable productId: UUID,
        @PathVariable variantId: UUID,
    ): AdminProductVariantResponse {
        return productVariantsService.getVariant(productId, variantId)?.toAdminResponse()
            ?: throw NotFoundException("Product variant not found")
    }

    @PostMapping("/{productId}/variants")
    fun upsertProductVariant(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpsertProductVariantRequest,
    ): AdminProductVariantResponse {
        return productVariantsService.upsertVariant(
            UpsertProductVariantCommand(
                id = request.id,
                productId = productId,
                externalId = request.externalId,
                sku = request.sku,
                title = request.title,
                priceMinor = request.priceMinor,
                oldPriceMinor = request.oldPriceMinor,
                imageIds = request.imageIds,
                sortOrder = request.sortOrder,
                isActive = request.isActive,
                optionValueIds = request.optionValueIds,
            )
        ).toAdminResponse()
    }

    @DeleteMapping("/{productId}/variants/{variantId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProductVariantImage(
        @PathVariable productId: UUID,
        @PathVariable variantId: UUID,
        @PathVariable imageId: UUID,
    ) {
        productVariantsService.deleteVariantImage(productId, variantId, imageId)
    }
}
