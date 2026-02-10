package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: CategoryEntity,

    @Column(name = "is_active")
    var isActive: Boolean = true
) : BaseAuditEntity<Long>()