package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
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

    @Column(name = "image_url")
    var imageUrl: String? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "images_products",
        joinColumns = [JoinColumn(name = "product_id", nullable = false)],
        inverseJoinColumns = [JoinColumn(name = "image_id", nullable = false)]
    )
    var images: MutableList<ImageEntity> = mutableListOf(),

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

    @Column(nullable = false)
    var score: Long = 0L,

    @Column(name = "show_in_collections", nullable = false)
    var showInCollections: Boolean = false,

    @Column(unique = true)
    var sku: String? = null,
) : BaseAuditEntity<Long>() {

    constructor(
        title: String,
        description: String? = null,
        price: BigDecimal,
        unit: UnitOfMeasure = UnitOfMeasure.PIECE,
        countStep: Int = 1,
        displayWeight: String?,
        category: CategoryEntity,
        isActive: Boolean = true,
        showInCollections: Boolean = false,
        sku: String? = null,
    ) : this(
        title = title,
        description = description,
        price = price,
        images = mutableListOf(),
        unit = unit,
        countStep = countStep,
        displayWeight = displayWeight,
        categories = mutableListOf(category),
        isActive = isActive,
        showInCollections = showInCollections,
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
