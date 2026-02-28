package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.data.entities.CategoryEntity

interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    fun findAllBySkuIn(skus: Collection<String>): List<CategoryEntity>

    @Transactional
    @Modifying
    @Query("update CategoryEntity c set c.isActive = false")
    fun markAllCategoriesAsInactive(): Int

    @Transactional
    @Modifying
    @Query("update CategoryEntity c set c.isActive = false where c.id=:categoryId")
    fun makeCategoryAsInactive(categoryId: Long)
}
