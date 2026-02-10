package ru.foodbox.delivery.data.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
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

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    val products: MutableList<ProductEntity> = mutableListOf(),

    @Column(name = "is_active")
    var isActive: Boolean = true,

) : BaseEntity<Long>()