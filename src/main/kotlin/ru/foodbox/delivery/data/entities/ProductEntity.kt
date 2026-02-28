package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import ru.foodbox.delivery.services.model.UnitOfMeasure
import java.math.BigDecimal

@Entity
@Table(name = "products")
class ProductEntity(
    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(nullable = false, precision = 19, scale = 2)
    var price: BigDecimal,

    var imageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    var unit: UnitOfMeasure = UnitOfMeasure.PIECE,

    @Column(name = "count_step")
    var countStep: Int = 1,

    @Column(name = "display_weight")
    var displayWeight: String?,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "products_categories",
        joinColumns = [JoinColumn(name = "product_id", nullable = false)],
        inverseJoinColumns = [JoinColumn(name = "category_id", nullable = false)],
    )
    var categories: MutableList<CategoryEntity> = mutableListOf(),

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(unique = true)
    var sku: String? = null,
) : BaseAuditEntity<Long>() {

    constructor(
        title: String,
        description: String? = null,
        price: BigDecimal,
        imageUrl: String? = null,
        unit: UnitOfMeasure = UnitOfMeasure.PIECE,
        countStep: Int = 1,
        displayWeight: String?,
        category: CategoryEntity,
        isActive: Boolean = true,
        sku: String? = null,
    ) : this(
        title = title,
        description = description,
        price = price,
        imageUrl = imageUrl,
        unit = unit,
        countStep = countStep,
        displayWeight = displayWeight,
        categories = mutableListOf(category),
        isActive = isActive,
        sku = sku,
    )

    @get:Transient
    var category: CategoryEntity
        get() = categories.firstOrNull()
            ?: error("ProductEntity(id=$id) has no categories")
        set(value) {
            categories = mutableListOf(value)
        }
}
