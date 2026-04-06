package ru.foodbox.delivery.modules.catalog.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductOptionGroupCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductOptionValueCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantOptionCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantsCommand
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionGroup
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionGroupDetails
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionValue
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductOptionValueDetails
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariant
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariantDetails
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariantOptionValue
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductOptionGroupRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductOptionValueRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantOptionValueRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantRepository
import java.time.Instant
import java.util.UUID

@Service
class CatalogProductVariantsService(
    private val optionGroupRepository: CatalogProductOptionGroupRepository,
    private val optionValueRepository: CatalogProductOptionValueRepository,
    private val variantRepository: CatalogProductVariantRepository,
    private val variantOptionValueRepository: CatalogProductVariantOptionValueRepository,
    private val productRepository: CatalogProductRepository,
    private val imageService: CatalogImageService,
) {

    fun findProductIdsWithVariants(productIds: Collection<UUID>): Set<UUID> {
        return variantRepository.findAllByProductIds(productIds)
            .mapTo(linkedSetOf()) { it.productId }
    }

    fun getDetails(productId: UUID): ProductVariantsDetails {
        val optionGroups = optionGroupRepository.findAllByProductId(productId)
        val optionGroupIds = optionGroups.map { it.id }
        val optionValues = optionValueRepository.findAllByOptionGroupIds(optionGroupIds)
        val optionValuesByGroupId = optionValues.groupBy { it.optionGroupId }

        val variants = variantRepository.findAllByProductId(productId)
        val variantIds = variants.map { it.id }
        val imagesByVariantId = imageService.getVariantCardImages(variantIds)
        val linksByVariantId = variantOptionValueRepository.findAllByVariantIds(variantIds)
            .groupBy { it.variantId }

        val optionGroupDetails = optionGroups.map { group ->
            CatalogProductOptionGroupDetails(
                id = group.id,
                code = group.code,
                title = group.title,
                sortOrder = group.sortOrder,
                values = optionValuesByGroupId[group.id].orEmpty().map { value ->
                    CatalogProductOptionValueDetails(
                        id = value.id,
                        code = value.code,
                        title = value.title,
                        sortOrder = value.sortOrder,
                    )
                },
            )
        }

        val variantDetails = variants.map { variant ->
            CatalogProductVariantDetails(
                id = variant.id,
                externalId = variant.externalId,
                sku = variant.sku,
                title = variant.title,
                priceMinor = variant.priceMinor,
                oldPriceMinor = variant.oldPriceMinor,
                imageIds = imagesByVariantId[variant.id].orEmpty().map { it.id },
                imageUrls = imagesByVariantId[variant.id].orEmpty().map { it.url },
                sortOrder = variant.sortOrder,
                isActive = variant.isActive,
                optionValueIds = linksByVariantId[variant.id].orEmpty().map { it.optionValueId },
            )
        }

        val defaultVariantId = variants.firstOrNull { it.isActive }?.id

        return ProductVariantsDetails(
            optionGroups = optionGroupDetails,
            defaultVariantId = defaultVariantId,
            variants = variantDetails,
        )
    }

    @Transactional
    fun replaceAll(
        productId: UUID,
        command: ReplaceProductVariantsCommand,
        now: Instant,
    ) {
        val normalizedGroups = normalizeGroups(command.optionGroups)
        val normalizedVariants = normalizeVariants(command.variants)

        validate(normalizedGroups, normalizedVariants, productId)

        val existingVariantIds = variantRepository.findAllByProductId(productId).map { it.id }
        val requestedVariantImageIds = normalizedVariants.flatMap { it.imageIds }
        imageService.validateVariantImages(
            existingVariantIds = existingVariantIds,
            requestedImageIds = requestedVariantImageIds,
        )
        imageService.detachVariantImages(
            existingVariantIds = existingVariantIds,
            retainedImageIds = requestedVariantImageIds,
            now = now,
        )

        deleteExistingData(productId, existingVariantIds)

        if (normalizedGroups.isEmpty() && normalizedVariants.isEmpty()) {
            return
        }

        val savedGroups = optionGroupRepository.saveAll(
            normalizedGroups.map { group ->
                CatalogProductOptionGroup(
                    id = UUID.randomUUID(),
                    productId = productId,
                    code = group.code,
                    title = group.title,
                    sortOrder = group.sortOrder,
                )
            }
        )
        val groupIdByCode = savedGroups.associate { it.code to it.id }

        val optionValueIdByGroupAndCode = linkedMapOf<Pair<String, String>, UUID>()
        val optionValuesToSave = normalizedGroups.flatMap { group ->
            group.values.map { value ->
                val optionValueId = UUID.randomUUID()
                optionValueIdByGroupAndCode[group.code to value.code] = optionValueId
                CatalogProductOptionValue(
                    id = optionValueId,
                    optionGroupId = groupIdByCode.getValue(group.code),
                    code = value.code,
                    title = value.title,
                    sortOrder = value.sortOrder,
                )
            }
        }
        optionValueRepository.saveAll(optionValuesToSave)

        val variantIdBySku = linkedMapOf<String, UUID>()
        val variantsToSave = normalizedVariants.map { variant ->
            val variantId = UUID.randomUUID()
            variantIdBySku[variant.sku] = variantId
            CatalogProductVariant(
                id = variantId,
                productId = productId,
                externalId = variant.externalId,
                sku = variant.sku,
                title = variant.title,
                priceMinor = variant.priceMinor,
                oldPriceMinor = variant.oldPriceMinor,
                imageUrls = emptyList(),
                sortOrder = variant.sortOrder,
                isActive = variant.isActive,
                createdAt = now,
                updatedAt = now,
            )
        }
        variantRepository.saveAll(variantsToSave)

        val linksToSave = normalizedVariants.flatMap { variant ->
            variant.options.map { option ->
                CatalogProductVariantOptionValue(
                    id = UUID.randomUUID(),
                    variantId = variantIdBySku.getValue(variant.sku),
                    optionGroupId = groupIdByCode.getValue(option.optionGroupCode),
                    optionValueId = optionValueIdByGroupAndCode.getValue(option.optionGroupCode to option.optionValueCode),
                )
            }
        }
        variantOptionValueRepository.saveAll(linksToSave)

        imageService.attachVariantImages(
            imageIdsByVariantId = normalizedVariants.associate { variant ->
                variantIdBySku.getValue(variant.sku) to variant.imageIds
            },
            now = now,
        )
    }

    private fun normalizeGroups(groups: List<ReplaceProductOptionGroupCommand>): List<NormalizedOptionGroup> {
        return groups.mapIndexed { index, group ->
            val code = group.code.trim().takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("optionGroups[$index].code is required")
            val title = group.title.trim().takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("optionGroups[$index].title is required")

            val normalizedValues = group.values.mapIndexed { valueIndex, value ->
                val valueCode = value.code.trim().takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("optionGroups[$index].values[$valueIndex].code is required")
                val valueTitle = value.title.trim().takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("optionGroups[$index].values[$valueIndex].title is required")

                NormalizedOptionValue(
                    code = valueCode,
                    title = valueTitle,
                    sortOrder = value.sortOrder,
                )
            }

            NormalizedOptionGroup(
                code = code,
                title = title,
                sortOrder = group.sortOrder,
                values = normalizedValues,
            )
        }
    }

    private fun normalizeVariants(variants: List<ReplaceProductVariantCommand>): List<NormalizedVariant> {
        return variants.mapIndexed { index, variant ->
            val sku = variant.sku.trim().takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("variants[$index].sku is required")
            val externalId = variant.externalId?.trim()?.takeIf { it.isNotBlank() }
            val title = variant.title?.trim()?.takeIf { it.isNotBlank() }
            if (variant.priceMinor != null && variant.priceMinor < 0) {
                throw IllegalArgumentException("variants[$index].priceMinor must be non-negative")
            }
            if (variant.oldPriceMinor != null && variant.oldPriceMinor < 0) {
                throw IllegalArgumentException("variants[$index].oldPriceMinor must be non-negative")
            }

            val options = variant.options.mapIndexed { optionIndex, option ->
                val optionGroupCode = option.optionGroupCode.trim().takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("variants[$index].options[$optionIndex].optionGroupCode is required")
                val optionValueCode = option.optionValueCode.trim().takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("variants[$index].options[$optionIndex].optionValueCode is required")

                NormalizedVariantOption(
                    optionGroupCode = optionGroupCode,
                    optionValueCode = optionValueCode,
                )
            }

            NormalizedVariant(
                externalId = externalId,
                sku = sku,
                title = title,
                priceMinor = variant.priceMinor,
                oldPriceMinor = variant.oldPriceMinor,
                imageIds = variant.imageIds,
                sortOrder = variant.sortOrder,
                isActive = variant.isActive,
                options = options,
            )
        }
    }

    private fun validate(
        optionGroups: List<NormalizedOptionGroup>,
        variants: List<NormalizedVariant>,
        productId: UUID,
    ) {
        validateOptionGroups(optionGroups)
        validateVariants(optionGroups, variants)
        validateGlobalSkuUniqueness(variants, productId)
    }

    private fun validateOptionGroups(optionGroups: List<NormalizedOptionGroup>) {
        val duplicateGroupCodes = optionGroups.groupBy { it.code }
            .filterValues { it.size > 1 }
            .keys
            .sorted()
        if (duplicateGroupCodes.isNotEmpty()) {
            throw IllegalArgumentException("Duplicate option group code(s): ${duplicateGroupCodes.joinToString(", ")}")
        }

        optionGroups.forEach { group ->
            if (group.values.isEmpty()) {
                throw IllegalArgumentException("Option group '${group.code}' must contain at least one value")
            }

            val duplicateValueCodes = group.values.groupBy { it.code }
                .filterValues { it.size > 1 }
                .keys
                .sorted()

            if (duplicateValueCodes.isNotEmpty()) {
                throw IllegalArgumentException(
                    "Duplicate option value code(s) in group '${group.code}': ${duplicateValueCodes.joinToString(", ")}",
                )
            }
        }
    }

    private fun validateVariants(optionGroups: List<NormalizedOptionGroup>, variants: List<NormalizedVariant>) {
        val duplicateSkus = variants.groupBy { it.sku }
            .filterValues { it.size > 1 }
            .keys
            .sorted()
        if (duplicateSkus.isNotEmpty()) {
            throw IllegalArgumentException("Duplicate variant sku(s): ${duplicateSkus.joinToString(", ")}")
        }

        if (optionGroups.isEmpty()) {
            val firstVariantWithOptions = variants.firstOrNull { it.options.isNotEmpty() }
            if (firstVariantWithOptions != null) {
                throw IllegalArgumentException("Variant '${firstVariantWithOptions.sku}' has options, but option groups are empty")
            }
            return
        }

        if (variants.isEmpty()) {
            throw IllegalArgumentException("At least one variant is required when option groups are provided")
        }

        val optionGroupByCode = optionGroups.associateBy { it.code }
        val optionGroupCodes = optionGroupByCode.keys

        variants.forEach { variant ->
            val duplicateGroupCodes = variant.options.groupBy { it.optionGroupCode }
                .filterValues { it.size > 1 }
                .keys
                .sorted()
            if (duplicateGroupCodes.isNotEmpty()) {
                throw IllegalArgumentException(
                    "Variant '${variant.sku}' contains multiple values for option group(s): ${duplicateGroupCodes.joinToString(", ")}",
                )
            }

            val providedGroupCodes = variant.options.map { it.optionGroupCode }.toSet()
            val unknownGroupCodes = providedGroupCodes - optionGroupCodes
            if (unknownGroupCodes.isNotEmpty()) {
                throw IllegalArgumentException(
                    "Variant '${variant.sku}' references unknown option group code(s): ${unknownGroupCodes.sorted().joinToString(", ")}",
                )
            }

            val missingGroupCodes = optionGroupCodes - providedGroupCodes
            if (missingGroupCodes.isNotEmpty()) {
                throw IllegalArgumentException(
                    "Variant '${variant.sku}' must contain exactly one value for each option group. Missing: ${missingGroupCodes.sorted().joinToString(", ")}",
                )
            }

            variant.options.forEach { option ->
                val group = optionGroupByCode.getValue(option.optionGroupCode)
                val valueExists = group.values.any { it.code == option.optionValueCode }
                if (!valueExists) {
                    throw IllegalArgumentException(
                        "Variant '${variant.sku}' references unknown option value '${option.optionValueCode}' in group '${option.optionGroupCode}'",
                    )
                }
            }
        }
    }

    private fun validateGlobalSkuUniqueness(variants: List<NormalizedVariant>, productId: UUID) {
        val skus = variants.map { it.sku }.toSet()
        if (skus.isEmpty()) {
            return
        }

        val firstVariantConflict = variantRepository.findAllBySkuIn(skus)
            .firstOrNull { it.productId != productId }
        if (firstVariantConflict != null) {
            throw IllegalArgumentException("Variant sku '${firstVariantConflict.sku}' already exists")
        }

        val productConflicts = productRepository.findAllBySkuIn(skus)
        val sameProductConflict = productConflicts.firstOrNull { it.id == productId }
        if (sameProductConflict != null) {
            throw IllegalArgumentException(
                "Variant sku '${sameProductConflict.sku}' conflicts with product sku of this product",
            )
        }

        val otherProductConflict = productConflicts.firstOrNull { it.id != productId }
        if (otherProductConflict != null) {
            throw IllegalArgumentException("Variant sku '${otherProductConflict.sku}' already exists")
        }
    }

    private fun deleteExistingData(productId: UUID, existingVariantIds: List<UUID>) {
        variantOptionValueRepository.deleteAllByVariantIds(existingVariantIds)
        variantRepository.deleteAllByProductId(productId)

        val existingOptionGroups = optionGroupRepository.findAllByProductId(productId)
        val existingOptionGroupIds = existingOptionGroups.map { it.id }
        optionValueRepository.deleteAllByOptionGroupIds(existingOptionGroupIds)
        optionGroupRepository.deleteAllByProductId(productId)
    }

    private data class NormalizedOptionGroup(
        val code: String,
        val title: String,
        val sortOrder: Int,
        val values: List<NormalizedOptionValue>,
    )

    private data class NormalizedOptionValue(
        val code: String,
        val title: String,
        val sortOrder: Int,
    )

    private data class NormalizedVariant(
        val externalId: String?,
        val sku: String,
        val title: String?,
        val priceMinor: Long?,
        val oldPriceMinor: Long?,
        val imageIds: List<UUID>,
        val sortOrder: Int,
        val isActive: Boolean,
        val options: List<NormalizedVariantOption>,
    )

    private data class NormalizedVariantOption(
        val optionGroupCode: String,
        val optionValueCode: String,
    )
}

data class ProductVariantsDetails(
    val optionGroups: List<CatalogProductOptionGroupDetails>,
    val defaultVariantId: UUID?,
    val variants: List<CatalogProductVariantDetails>,
)
