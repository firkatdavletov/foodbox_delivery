package ru.foodbox.delivery.modules.catalog.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantsCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertCategoryCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductCommand
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductDetails
import ru.foodbox.delivery.modules.catalog.domain.ProductSnapshot
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogProductModifiersService
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryImageRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductImageRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Service
class CatalogServiceImpl(
    private val categoryRepository: CatalogCategoryRepository,
    private val productRepository: CatalogProductRepository,
    private val categoryImageRepository: CatalogCategoryImageRepository,
    private val productImageRepository: CatalogProductImageRepository,
    private val productVariantsService: CatalogProductVariantsService,
    private val productModifiersService: CatalogProductModifiersService,
    private val imageService: CatalogImageService,
) : CatalogService, ProductReadService {

    override fun getCategories(activeOnly: Boolean): List<CatalogCategory> {
        return enrichCategories(categoryRepository.findAll(activeOnly))
    }

    override fun getProducts(categoryId: UUID?, query: String?): List<CatalogProduct> {
        val products = productRepository.findAllActive(
            categoryId = categoryId,
            query = query?.trim()?.takeIf { it.isNotBlank() },
        )
        return enrichProducts(products, configurationActiveOnly = true)
    }

    override fun getAdminCategories(isActive: Boolean): List<CatalogCategory> {
        return enrichCategories(categoryRepository.findAllByIsActive(isActive))
    }

    override fun getAdminCategoryDetails(categoryId: UUID): CatalogCategory? {
        val category = categoryRepository.findById(categoryId) ?: return null
        return enrichCategories(listOf(category)).first()
    }

    override fun getAdminProducts(isActive: Boolean): List<CatalogProduct> {
        return enrichProducts(productRepository.findAllByIsActive(isActive), configurationActiveOnly = false)
    }

    override fun getProductDetails(productId: UUID): CatalogProductDetails? {
        val product = productRepository.findActiveById(productId) ?: return null

        return buildProductDetails(
            product = product,
            variantDetails = productVariantsService.getActiveDetails(product.id),
            modifierGroupsActiveOnly = true,
        )
    }

    override fun getAdminProductDetails(productId: UUID): CatalogProductDetails? {
        val product = productRepository.findById(productId) ?: return null
        return buildProductDetails(
            product = product,
            variantDetails = productVariantsService.getDetails(product.id),
            modifierGroupsActiveOnly = false,
        )
    }

    override fun getActiveProductSnapshot(productId: UUID, variantId: UUID?): ProductSnapshot? {
        val product = productRepository.findActiveById(productId) ?: return null

        val variantDetails = productVariantsService.getActiveDetails(product.id).variants
        val resolvedVariant = when {
            variantDetails.isEmpty() -> null
            variantId == null -> variantDetails.first()
            else -> variantDetails.firstOrNull { it.id == variantId } ?: return null
        }

        val resolvedTitle = resolvedVariant?.title?.takeIf { it.isNotBlank() }?.let {
            "${product.title} ($it)"
        } ?: product.title

        return ProductSnapshot(
            id = product.id,
            variantId = resolvedVariant?.id,
            sku = resolvedVariant?.sku ?: product.sku,
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
            externalId = resolveOptionalStringUpdate(command.externalId, existing.externalId),
            name = command.name.trim(),
            slug = normalizeSlug(command.slug, command.name),
            description = resolveOptionalStringUpdate(command.description, existing.description),
            sortOrder = command.sortOrder ?: existing.sortOrder,
            imageUrls = existing.imageUrls,
            isActive = command.isActive,
            updatedAt = now,
        ) ?: CatalogCategory(
            id = command.id ?: UUID.randomUUID(),
            externalId = normalizeOptionalString(command.externalId),
            name = command.name.trim(),
            slug = normalizeSlug(command.slug, command.name),
            description = normalizeOptionalString(command.description),
            sortOrder = command.sortOrder ?: 0,
            imageUrls = emptyList(),
            isActive = command.isActive,
            createdAt = now,
            updatedAt = now,
        )

        val saved = categoryRepository.save(category)
        imageService.syncCategoryImages(saved.id, command.imageIds, now)
        return enrichCategories(listOf(saved)).first()
    }

    @Transactional
    override fun deleteCategoryImage(categoryId: UUID, imageId: UUID) {
        categoryRepository.findById(categoryId)
            ?: throw NotFoundException("Category not found")

        val existingImages = categoryImageRepository.findAllByCategoryIds(listOf(categoryId))
        val remainingImageIds = existingImages
            .filterNot { it.imageId == imageId }
            .map { it.imageId }

        if (remainingImageIds.size == existingImages.size) {
            throw NotFoundException("Category image not found")
        }

        imageService.syncCategoryImages(categoryId, remainingImageIds, Instant.now())
    }

    private fun buildProductDetails(
        product: CatalogProduct,
        variantDetails: ProductVariantsDetails,
        modifierGroupsActiveOnly: Boolean,
    ): CatalogProductDetails {
        val modifierGroups = productModifiersService.getProductModifierGroups(
            productId = product.id,
            activeOnly = modifierGroupsActiveOnly,
        )
        val productImages = imageService.getProductCardImages(listOf(product.id))[product.id].orEmpty()
        return CatalogProductDetails(
            product = product.copy(
                imageUrls = productImages.map { it.url },
                isConfigured = variantDetails.variants.isNotEmpty() || modifierGroups.isNotEmpty(),
            ),
            imageIds = productImages.map { it.id },
            optionGroups = variantDetails.optionGroups,
            modifierGroups = modifierGroups,
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
            imageUrls = existing.imageUrls,
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
            imageUrls = emptyList(),
            sortOrder = command.sortOrder ?: 0,
            unit = command.unit,
            countStep = command.countStep,
            isActive = command.isActive,
            createdAt = now,
            updatedAt = now,
        )

        val saved = productRepository.save(product)
        imageService.syncProductImages(saved.id, command.imageIds, now)
        if (command.replaceProductVariants) {
            productVariantsService.replaceAll(
                productId = saved.id,
                command = ReplaceProductVariantsCommand(
                    optionGroups = command.optionGroups,
                    variants = command.variants,
                ),
                now = now,
            )
        }
        if (command.replaceProductModifierGroups) {
            productModifiersService.replaceProductModifierGroups(
                productId = saved.id,
                commands = command.modifierGroups,
            )
        }
        return enrichProducts(listOf(saved), configurationActiveOnly = false).first()
    }

    @Transactional
    override fun deleteProductImage(productId: UUID, imageId: UUID) {
        productRepository.findById(productId)
            ?: throw NotFoundException("Product not found")

        val existingImages = productImageRepository.findAllByProductIds(listOf(productId))
        val remainingImageIds = existingImages
            .filterNot { it.imageId == imageId }
            .map { it.imageId }

        if (remainingImageIds.size == existingImages.size) {
            throw NotFoundException("Product image not found")
        }

        imageService.syncProductImages(productId, remainingImageIds, Instant.now())
    }

    private fun enrichCategories(categories: List<CatalogCategory>): List<CatalogCategory> {
        if (categories.isEmpty()) {
            return emptyList()
        }

        val imagesByCategoryId = imageService.getCategoryImages(categories.map { it.id })
        return categories.map { category ->
            val images = imagesByCategoryId[category.id].orEmpty()
            category.copy(
                imageIds = images.map { it.id },
                imageUrls = images.map { it.url },
            )
        }
    }

    private fun enrichProducts(products: List<CatalogProduct>, configurationActiveOnly: Boolean): List<CatalogProduct> {
        if (products.isEmpty()) {
            return emptyList()
        }

        val productIds = products.map { it.id }
        val imageUrlsByProductId = imageService.getProductThumbUrls(products.map { it.id })
        val configuredProductIds = buildSet {
            addAll(productVariantsService.findProductIdsWithVariants(productIds, activeOnly = configurationActiveOnly))
            addAll(
                productModifiersService.findProductIdsWithModifierGroups(
                    productIds = productIds,
                    activeOnly = configurationActiveOnly,
                )
            )
        }
        return products.map { product ->
            product.copy(
                imageUrls = imageUrlsByProductId[product.id].orEmpty(),
                isConfigured = product.id in configuredProductIds,
            )
        }
    }

    private fun normalizeSlug(rawSlug: String?, fallback: String): String {
        val base = rawSlug?.trim()?.takeIf { it.isNotBlank() } ?: fallback
        return base
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { UUID.randomUUID().toString() }
    }

    private fun resolveOptionalStringUpdate(incoming: String?, existing: String?): String? {
        return if (incoming == null) {
            existing
        } else {
            normalizeOptionalString(incoming)
        }
    }

    private fun normalizeOptionalString(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotBlank() }
    }
}
