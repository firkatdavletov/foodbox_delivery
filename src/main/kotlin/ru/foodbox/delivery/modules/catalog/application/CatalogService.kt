package ru.foodbox.delivery.modules.catalog.application

import ru.foodbox.delivery.modules.catalog.application.command.UpsertCategoryCommand
import ru.foodbox.delivery.modules.catalog.application.command.UpsertProductCommand
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductDetails
import java.util.UUID

interface CatalogService {
    fun getCategories(activeOnly: Boolean = true): List<CatalogCategory>
    fun getProducts(categoryId: UUID? = null, query: String? = null): List<CatalogProduct>
    fun getAdminCategories(isActive: Boolean): List<CatalogCategory>
    fun getAdminCategoryDetails(categoryId: UUID): CatalogCategory?
    fun getAdminProducts(isActive: Boolean): List<CatalogProduct>
    fun getProductDetails(productId: UUID): CatalogProductDetails?
    fun getAdminProductDetails(productId: UUID): CatalogProductDetails?
    fun upsertCategory(command: UpsertCategoryCommand): CatalogCategory
    fun upsertProduct(command: UpsertProductCommand): CatalogProduct
    fun deleteCategoryImage(categoryId: UUID, imageId: UUID)
    fun deleteProductImage(productId: UUID, imageId: UUID)
}
