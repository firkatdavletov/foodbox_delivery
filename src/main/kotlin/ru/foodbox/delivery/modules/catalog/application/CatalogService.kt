package ru.foodbox.delivery.modules.catalog.application

import ru.foodbox.delivery.modules.catalog.application.command.UpsertCategoryCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductCommand
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import java.util.UUID

interface CatalogService {
    fun getCategories(activeOnly: Boolean = true): List<CatalogCategory>
    fun getProducts(categoryId: UUID? = null, query: String? = null): List<CatalogProduct>
    fun getProduct(productId: UUID): CatalogProduct?
    fun upsertCategory(command: UpsertCategoryCommand): CatalogCategory
    fun upsertProduct(command: UpsertProductCommand): CatalogProduct
}
