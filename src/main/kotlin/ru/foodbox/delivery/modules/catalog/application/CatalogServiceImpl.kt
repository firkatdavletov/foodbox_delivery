package ru.foodbox.delivery.modules.catalog.application

import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.application.command.UpsertCategoryCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductCommand
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.ProductSnapshot
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Service
class CatalogServiceImpl(
    private val categoryRepository: CatalogCategoryRepository,
    private val productRepository: CatalogProductRepository,
) : CatalogService, ProductReadService {

    override fun getCategories(activeOnly: Boolean): List<CatalogCategory> {
        return categoryRepository.findAll(activeOnly)
    }

    override fun getProducts(categoryId: UUID?, query: String?): List<CatalogProduct> {
        return productRepository.findAllActive(
            categoryId = categoryId,
            query = query?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    override fun getProduct(productId: UUID): CatalogProduct? {
        val product = productRepository.findById(productId) ?: return null
        return if (product.isActive) product else null
    }

    override fun getActiveProductSnapshot(productId: UUID): ProductSnapshot? {
        val product = productRepository.findById(productId) ?: return null
        if (!product.isActive) {
            return null
        }

        return ProductSnapshot(
            id = product.id,
            title = product.title,
            unit = product.unit,
            countStep = product.countStep,
            priceMinor = product.priceMinor,
        )
    }

    override fun upsertCategory(command: UpsertCategoryCommand): CatalogCategory {
        val now = Instant.now()
        val existing = command.id?.let(categoryRepository::findById)

        val category = existing?.copy(
            name = command.name.trim(),
            slug = normalizeSlug(command.slug, command.name),
            imageUrl = command.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            isActive = command.isActive,
            updatedAt = now,
        ) ?: CatalogCategory(
            id = command.id ?: UUID.randomUUID(),
            name = command.name.trim(),
            slug = normalizeSlug(command.slug, command.name),
            imageUrl = command.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            isActive = command.isActive,
            createdAt = now,
            updatedAt = now,
        )

        return categoryRepository.save(category)
    }

    override fun upsertProduct(command: UpsertProductCommand): CatalogProduct {
        val category = categoryRepository.findById(command.categoryId)
            ?: throw NotFoundException("Category not found")

        if (command.countStep <= 0) {
            throw IllegalArgumentException("countStep must be greater than zero")
        }

        if (command.priceMinor < 0) {
            throw IllegalArgumentException("priceMinor must be positive")
        }

        if (!category.isActive) {
            throw IllegalArgumentException("Category is inactive")
        }

        val now = Instant.now()
        val existing = command.id?.let(productRepository::findById)

        val product = existing?.copy(
            categoryId = command.categoryId,
            title = command.title.trim(),
            slug = normalizeSlug(command.slug, command.title),
            description = command.description?.trim()?.takeIf { it.isNotBlank() },
            priceMinor = command.priceMinor,
            oldPriceMinor = command.oldPriceMinor,
            sku = command.sku?.trim()?.takeIf { it.isNotBlank() },
            imageUrl = command.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            unit = command.unit,
            countStep = command.countStep,
            isActive = command.isActive,
            updatedAt = now,
        ) ?: CatalogProduct(
            id = command.id ?: UUID.randomUUID(),
            categoryId = command.categoryId,
            title = command.title.trim(),
            slug = normalizeSlug(command.slug, command.title),
            description = command.description?.trim()?.takeIf { it.isNotBlank() },
            priceMinor = command.priceMinor,
            oldPriceMinor = command.oldPriceMinor,
            sku = command.sku?.trim()?.takeIf { it.isNotBlank() },
            imageUrl = command.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            unit = command.unit,
            countStep = command.countStep,
            isActive = command.isActive,
            createdAt = now,
            updatedAt = now,
        )

        return productRepository.save(product)
    }

    private fun normalizeSlug(rawSlug: String?, fallback: String): String {
        val base = rawSlug?.trim()?.takeIf { it.isNotBlank() } ?: fallback
        return base
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { UUID.randomUUID().toString() }
    }
}
