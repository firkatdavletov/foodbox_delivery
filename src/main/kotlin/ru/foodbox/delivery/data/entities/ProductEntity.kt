package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "products")
class ProductEntity(
    @Column(nullable = false)
    var title: String,

    var description: String? = null,

    @Column(nullable = false)
    var price: Double,

    var imageUrl: String? = null,

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    val category: CategoryEntity
) : BaseAuditEntity<Long>()