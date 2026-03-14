package ru.foodbox.delivery.modules.catalog.domain

import java.util.UUID

data class CatalogProductVariantOptionValue(
    val id: UUID,
    val variantId: UUID,
    val optionGroupId: UUID,
    val optionValueId: UUID,
)
