package ru.foodbox.delivery.modules.catalog.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductOptionGroupCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductOptionValueCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantOptionCommand
import ru.foodbox.delivery.modules.catalog.application.command.ReplaceProductVariantsCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductOptionGroupCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductOptionValueCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductVariantCommand
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

    fun getOptionGroup(productId: UUID, optionGroupId: UUID): CatalogProductOptionGroupDetails? {
        return getDetails(productId).optionGroups.firstOrNull { it.id == optionGroupId }
    }

    fun getVariant(productId: UUID, variantId: UUID): CatalogProductVariantDetails? {
        val variant = variantRepository.findById(variantId) ?: return null
        if (variant.productId != productId) {
            return null
        }

        return getDetails(productId).variants.firstOrNull { it.id == variantId }
    }

    @Transactional
    fun upsertOptionGroup(command: UpsertProductOptionGroupCommand): CatalogProductOptionGroupDetails {
        productRepository.findById(command.productId)
            ?: throw NotFoundException("Product not found")

        val normalizedCode = command.code.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("optionGroup.code is required")
        val normalizedTitle = command.title.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("optionGroup.title is required")

        val existingGroups = optionGroupRepository.findAllByProductId(command.productId)
        val existingGroup = command.id?.let { groupId ->
            existingGroups.firstOrNull { it.id == groupId }
                ?: throw NotFoundException("Option group not found")
        }

        val duplicateCode = existingGroups.firstOrNull { it.id != existingGroup?.id && it.code == normalizedCode }
        if (duplicateCode != null) {
            throw IllegalArgumentException("Option group code '$normalizedCode' already exists")
        }

        val optionGroupId = existingGroup?.id ?: UUID.randomUUID()
        optionGroupRepository.saveAll(
            listOf(
                CatalogProductOptionGroup(
                    id = optionGroupId,
                    productId = command.productId,
                    code = normalizedCode,
                    title = normalizedTitle,
                    sortOrder = command.sortOrder,
                )
            )
        )

        return getOptionGroup(command.productId, optionGroupId)
            ?: throw IllegalStateException("Saved option group was not found")
    }

    @Transactional
    fun upsertOptionValue(command: UpsertProductOptionValueCommand): CatalogProductOptionValueDetails {
        productRepository.findById(command.productId)
            ?: throw NotFoundException("Product not found")

        val optionGroup = optionGroupRepository.findAllByProductId(command.productId)
            .firstOrNull { it.id == command.optionGroupId }
            ?: throw NotFoundException("Option group not found")

        val normalizedCode = command.code.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("optionValue.code is required")
        val normalizedTitle = command.title.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("optionValue.title is required")

        val existingValues = optionValueRepository.findAllByOptionGroupIds(listOf(command.optionGroupId))
        val existingValue = command.id?.let { optionValueId ->
            existingValues.firstOrNull { it.id == optionValueId }
                ?: throw NotFoundException("Option value not found")
        }

        val duplicateCode = existingValues.firstOrNull { it.id != existingValue?.id && it.code == normalizedCode }
        if (duplicateCode != null) {
            throw IllegalArgumentException("Option value code '$normalizedCode' already exists in group '${optionGroup.code}'")
        }

        val optionValueId = existingValue?.id ?: UUID.randomUUID()
        optionValueRepository.saveAll(
            listOf(
                CatalogProductOptionValue(
                    id = optionValueId,
                    optionGroupId = optionGroup.id,
                    code = normalizedCode,
                    title = normalizedTitle,
                    sortOrder = command.sortOrder,
                )
            )
        )

        return getOptionGroup(command.productId, optionGroup.id)
            ?.values
            ?.firstOrNull { it.id == optionValueId }
            ?: throw IllegalStateException("Saved option value was not found")
    }

    @Transactional
    fun upsertVariant(command: UpsertProductVariantCommand): CatalogProductVariantDetails {
        productRepository.findById(command.productId)
            ?: throw NotFoundException("Product not found")

        val existingVariant = command.id?.let { variantId ->
            variantRepository.findById(variantId)
                ?.takeIf { it.productId == command.productId }
                ?: throw NotFoundException("Product variant not found")
        }

        val normalizedVariant = normalizeVariant(command)
        validateVariantSkuUniqueness(
            sku = normalizedVariant.sku,
            productId = command.productId,
            currentVariantId = existingVariant?.id,
        )

        val optionGroups = optionGroupRepository.findAllByProductId(command.productId)
        val optionValues = optionValueRepository.findAllByOptionGroupIds(optionGroups.map { it.id })
        val selectedOptionValues = resolveSelectedOptionValues(
            sku = normalizedVariant.sku,
            optionGroups = optionGroups,
            optionValues = optionValues,
            optionValueIds = normalizedVariant.optionValueIds,
        )

        val variantId = existingVariant?.id ?: UUID.randomUUID()
        val now = Instant.now()
        imageService.validateVariantImages(
            existingVariantIds = existingVariant?.let { listOf(it.id) }.orEmpty(),
            requestedImageIds = normalizedVariant.imageIds,
        )
        imageService.detachVariantImages(
            existingVariantIds = existingVariant?.let { listOf(it.id) }.orEmpty(),
            retainedImageIds = normalizedVariant.imageIds,
            now = now,
        )

        variantOptionValueRepository.deleteAllByVariantIds(listOf(variantId))
        variantRepository.saveAll(
            listOf(
                CatalogProductVariant(
                    id = variantId,
                    productId = command.productId,
                    externalId = normalizedVariant.externalId,
                    sku = normalizedVariant.sku,
                    title = normalizedVariant.title,
                    priceMinor = normalizedVariant.priceMinor,
                    oldPriceMinor = normalizedVariant.oldPriceMinor,
                    imageUrls = emptyList(),
                    sortOrder = normalizedVariant.sortOrder,
                    isActive = normalizedVariant.isActive,
                    createdAt = existingVariant?.createdAt ?: now,
                    updatedAt = now,
                )
            )
        )

        variantOptionValueRepository.saveAll(
            selectedOptionValues.map { optionValue ->
                CatalogProductVariantOptionValue(
                    id = UUID.randomUUID(),
                    variantId = variantId,
                    optionGroupId = optionValue.optionGroupId,
                    optionValueId = optionValue.id,
                )
            }
        )

        imageService.attachVariantImages(
            imageIdsByVariantId = mapOf(variantId to normalizedVariant.imageIds),
            now = now,
        )

        return getVariant(command.productId, variantId)
            ?: throw IllegalStateException("Saved variant was not found")
    }

    @Transactional
    fun deleteVariantImage(productId: UUID, variantId: UUID, imageId: UUID) {
        productRepository.findById(productId)
            ?: throw NotFoundException("Product not found")

        variantRepository.findById(variantId)
            ?.takeIf { it.productId == productId }
            ?: throw NotFoundException("Product variant not found")

        val variantImages = imageService.getVariantImages(listOf(variantId))[variantId].orEmpty()
        val remainingImageIds = variantImages
            .filterNot { it.id == imageId }
            .map { it.id }

        if (remainingImageIds.size == variantImages.size) {
            throw NotFoundException("Variant image not found")
        }

        val now = Instant.now()
        imageService.detachVariantImages(
            existingVariantIds = listOf(variantId),
            retainedImageIds = remainingImageIds,
            now = now,
        )
        if (remainingImageIds.isNotEmpty()) {
            imageService.attachVariantImages(
                imageIdsByVariantId = mapOf(variantId to remainingImageIds),
                now = now,
            )
        }
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

    private fun normalizeVariant(command: UpsertProductVariantCommand): NormalizedVariantSelection {
        val sku = command.sku.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("variant.sku is required")
        val externalId = command.externalId?.trim()?.takeIf { it.isNotBlank() }
        val title = command.title?.trim()?.takeIf { it.isNotBlank() }

        if (command.priceMinor != null && command.priceMinor < 0) {
            throw IllegalArgumentException("variant.priceMinor must be non-negative")
        }
        if (command.oldPriceMinor != null && command.oldPriceMinor < 0) {
            throw IllegalArgumentException("variant.oldPriceMinor must be non-negative")
        }

        return NormalizedVariantSelection(
            externalId = externalId,
            sku = sku,
            title = title,
            priceMinor = command.priceMinor,
            oldPriceMinor = command.oldPriceMinor,
            imageIds = command.imageIds,
            sortOrder = command.sortOrder,
            isActive = command.isActive,
            optionValueIds = command.optionValueIds,
        )
    }

    private fun resolveSelectedOptionValues(
        sku: String,
        optionGroups: List<CatalogProductOptionGroup>,
        optionValues: List<CatalogProductOptionValue>,
        optionValueIds: List<UUID>,
    ): List<CatalogProductOptionValue> {
        if (optionGroups.isEmpty()) {
            if (optionValueIds.isNotEmpty()) {
                throw IllegalArgumentException("Variant '$sku' has options, but option groups are empty")
            }
            return emptyList()
        }

        val optionValuesById = optionValues.associateBy { it.id }
        val selectedValues = optionValueIds.map { optionValueId ->
            optionValuesById[optionValueId]
                ?: throw IllegalArgumentException("Variant '$sku' references unknown option value id '$optionValueId'")
        }

        val duplicateGroupCodes = selectedValues.groupBy { it.optionGroupId }
            .filterValues { it.size > 1 }
            .keys
            .mapNotNull { groupId -> optionGroups.firstOrNull { it.id == groupId }?.code }
            .sorted()
        if (duplicateGroupCodes.isNotEmpty()) {
            throw IllegalArgumentException(
                "Variant '$sku' contains multiple values for option group(s): ${duplicateGroupCodes.joinToString(", ")}",
            )
        }

        val providedGroupIds = selectedValues.mapTo(linkedSetOf()) { it.optionGroupId }
        val missingGroupCodes = optionGroups
            .filterNot { it.id in providedGroupIds }
            .map { it.code }
            .sorted()
        if (missingGroupCodes.isNotEmpty()) {
            throw IllegalArgumentException(
                "Variant '$sku' must contain exactly one value for each option group. Missing: ${missingGroupCodes.joinToString(", ")}",
            )
        }

        return selectedValues
    }

    private fun validateVariantSkuUniqueness(
        sku: String,
        productId: UUID,
        currentVariantId: UUID?,
    ) {
        val existingVariantConflict = variantRepository.findAllBySkuIn(setOf(sku))
            .firstOrNull { it.id != currentVariantId }
        if (existingVariantConflict != null) {
            throw IllegalArgumentException("Variant sku '${existingVariantConflict.sku}' already exists")
        }

        val productConflict = productRepository.findAllBySkuIn(setOf(sku)).firstOrNull()
        if (productConflict != null && productConflict.id == productId) {
            throw IllegalArgumentException("Variant sku '${productConflict.sku}' conflicts with product sku of this product")
        }
        if (productConflict != null && productConflict.id != productId) {
            throw IllegalArgumentException("Variant sku '${productConflict.sku}' already exists")
        }
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

    private data class NormalizedVariantSelection(
        val externalId: String?,
        val sku: String,
        val title: String?,
        val priceMinor: Long?,
        val oldPriceMinor: Long?,
        val imageIds: List<UUID>,
        val sortOrder: Int,
        val isActive: Boolean,
        val optionValueIds: List<UUID>,
    )
}

data class ProductVariantsDetails(
    val optionGroups: List<CatalogProductOptionGroupDetails>,
    val defaultVariantId: UUID?,
    val variants: List<CatalogProductVariantDetails>,
)
