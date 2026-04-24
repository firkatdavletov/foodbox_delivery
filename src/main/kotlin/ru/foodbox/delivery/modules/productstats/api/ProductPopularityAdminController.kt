package ru.foodbox.delivery.modules.productstats.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.catalog.application.CatalogService
import ru.foodbox.delivery.modules.productstats.api.dto.ProductPopularityAdminItemResponse
import ru.foodbox.delivery.modules.productstats.api.dto.ProductPopularityStatsResponse
import ru.foodbox.delivery.modules.productstats.api.dto.ReorderProductPopularityRequest
import ru.foodbox.delivery.modules.productstats.api.dto.UpsertProductPopularityRequest
import ru.foodbox.delivery.modules.productstats.application.ProductPopularityService
import ru.foodbox.delivery.modules.productstats.application.command.ReorderProductPopularityCommand
import ru.foodbox.delivery.modules.productstats.application.command.UpsertProductPopularityCommand
import ru.foodbox.delivery.modules.productstats.domain.ProductPopularityStats
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/product-stats/popularity")
class ProductPopularityAdminController(
    private val productPopularityService: ProductPopularityService,
    private val catalogService: CatalogService,
) {

    @GetMapping
    fun getPopularProducts(): List<ProductPopularityAdminItemResponse> {
        return productPopularityService.getEnabledStats().toAdminItemResponses()
    }

    @PutMapping("/reorder")
    fun reorderPopularProducts(
        @Valid @RequestBody request: ReorderProductPopularityRequest,
    ): List<ProductPopularityAdminItemResponse> {
        return productPopularityService.reorder(
            ReorderProductPopularityCommand(
                productIds = request.productIds,
            )
        ).toAdminItemResponses()
    }

    @GetMapping("/{productId}")
    fun getProductPopularity(
        @PathVariable productId: UUID,
    ): ProductPopularityStatsResponse {
        return productPopularityService.getStats(productId).toResponse(productId)
    }

    @PutMapping("/{productId}")
    fun upsertProductPopularity(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpsertProductPopularityRequest,
    ): ProductPopularityStatsResponse {
        return productPopularityService.upsertStats(
            productId = productId,
            command = UpsertProductPopularityCommand(
                enabled = request.enabled,
                manualScore = request.manualScore,
            ),
        ).toResponse(productId)
    }

    private fun List<ProductPopularityStats>.toAdminItemResponses(): List<ProductPopularityAdminItemResponse> {
        val productsById = catalogService.getAdminProductsByIds(map { it.productId }).associateBy { it.id }
        return mapNotNull { stats ->
            productsById[stats.productId]?.let(stats::toAdminItemResponse)
        }
    }
}
