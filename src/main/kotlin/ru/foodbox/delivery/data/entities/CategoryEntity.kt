package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
data class CategoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val title: String,
    @Column(name = "parent_category_id")
    val parentCategoryId: Long? = null,
    val imageUrl: String? = null,
)