package ru.foodbox.delivery.data.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
class CategoryEntity(

    var title: String,

    @Column(name = "parent_category_id")
    var parentCategoryId: Long? = null,

    var imageUrl: String? = null,

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    val products: MutableList<ProductEntity>,

    val span: Int = 1

) : BaseEntity<Long>()