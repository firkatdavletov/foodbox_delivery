package ru.foodbox.delivery.modules.catalog.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantsCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertCategoryCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductCommand
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductDetails
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
    private val productVariantsService: CatalogProductVariantsService,
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

    override fun getAdminCategories(isActive: Boolean): List<CatalogCategory> {
        return categoryRepository.findAllByIsActive(isActive)
    }

    override fun getAdminProducts(isActive: Boolean): List<CatalogProduct> {
        return productRepository.findAllByIsActive(isActive)
    }

    override fun getProductDetails(productId: UUID): CatalogProductDetails? {
        val product = productRepository.findById(productId) ?: return null
        if (!product.isActive) {
            return null
        }

        return buildProductDetails(product.id)
    }

    override fun getAdminProductDetails(productId: UUID): CatalogProductDetails? {
        val product = productRepository.findById(productId) ?: return null
        return buildProductDetails(product.id)
    }

    override fun getActiveProductSnapshot(productId: UUID, variantId: UUID?): ProductSnapshot? {
        val product = productRepository.findById(productId) ?: return null
        if (!product.isActive) {
            return null
        }

        val variantDetails = productVariantsService.getDetails(product.id).variants
        val resolvedVariant = when {
            variantDetails.isEmpty() -> null
            else -> {
                val activeVariants = variantDetails.filter { it.isActive }
                if (activeVariants.isEmpty()) {
                    return null
                }

                if (variantId == null) {
                    activeVariants.first()
                } else {
                    activeVariants.firstOrNull { it.id == variantId } ?: return null
                }
            }
        }

        val resolvedTitle = resolvedVariant?.title?.takeIf { it.isNotBlank() }?.let {
            "${product.title} ($it)"
        } ?: product.title

        return ProductSnapshot(
            id = product.id,
            variantId = resolvedVariant?.id,
            title = resolvedTitle,
            unit = product.unit,
            countStep = product.countStep,
            priceMinor = resolvedVariant?.priceMinor ?: product.priceMinor,
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

    private fun buildProductDetails(productId: UUID): CatalogProductDetails {
        val product = productRepository.findById(productId)
            ?: throw NotFoundException("Product not found")
        val variantDetails = productVariantsService.getDetails(product.id)
        return CatalogProductDetails(
            product = product,
            optionGroups = variantDetails.optionGroups,
            defaultVariantId = variantDetails.defaultVariantId,
            variants = variantDetails.variants,
        )
    }

    @Transactional
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
            externalId = command.externalId?.trim()?.takeIf { it.isNotBlank() } ?: existing.externalId,
            categoryId = command.categoryId,
            title = command.title.trim(),
            slug = normalizeSlug(command.slug, command.title),
            description = command.description?.trim()?.takeIf { it.isNotBlank() },
            priceMinor = command.priceMinor,
            oldPriceMinor = command.oldPriceMinor,
            sku = command.sku?.trim()?.takeIf { it.isNotBlank() },
            brand = command.brand?.trim()?.takeIf { it.isNotBlank() } ?: existing.brand,
            imageUrl = command.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            sortOrder = command.sortOrder ?: existing.sortOrder,
            unit = command.unit,
            countStep = command.countStep,
            isActive = command.isActive,
            updatedAt = now,
        ) ?: CatalogProduct(
            id = command.id ?: UUID.randomUUID(),
            externalId = command.externalId?.trim()?.takeIf { it.isNotBlank() },
            categoryId = command.categoryId,
            title = command.title.trim(),
            slug = normalizeSlug(command.slug, command.title),
            description = command.description?.trim()?.takeIf { it.isNotBlank() },
            priceMinor = command.priceMinor,
            oldPriceMinor = command.oldPriceMinor,
            sku = command.sku?.trim()?.takeIf { it.isNotBlank() },
            brand = command.brand?.trim()?.takeIf { it.isNotBlank() },
            imageUrl = command.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            sortOrder = command.sortOrder ?: 0,
            unit = command.unit,
            countStep = command.countStep,
            isActive = command.isActive,
            createdAt = now,
            updatedAt = now,
        )

        val saved = productRepository.save(product)
        productVariantsService.replaceAll(
            productId = saved.id,
            command = ReplaceProductVariantsCommand(
                optionGroups = command.optionGroups,
                variants = command.variants,
            ),
            now = now,
        )
        return saved
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
