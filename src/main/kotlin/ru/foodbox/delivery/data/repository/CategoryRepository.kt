package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.CategoryEntity

interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    fun findAllByParentCategoryId(parentCategoryId: Long?): List<CategoryEntity>
}