package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.ProductEntity

interface ProductRepository : JpaRepository<ProductEntity, Long> {
    fun findAllByCategoryId(categoryId: Long): List<ProductEntity>
    fun findByTitleContainingIgnoreCase(keyword: String): List<ProductEntity>
    fun findById(ids: Array<Long>): List<ProductEntity>
}