package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
class CategoryEntity(

    var title: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    var parent: CategoryEntity? = null,

    @OneToMany(mappedBy = "parent")
    val children: MutableList<CategoryEntity> = mutableListOf(),

    var imageUrl: String? = null,

    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    val products: MutableList<ProductEntity> = mutableListOf(),

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(unique = true)
    var sku: String? = null,

) : BaseEntity<Long>()
