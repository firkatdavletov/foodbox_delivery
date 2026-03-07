package ru.foodbox.delivery.data.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.data.entities.ProductEntity
import java.time.LocalDateTime

interface ProductRepository : JpaRepository<ProductEntity, Long> {
    fun findAllBySkuIn(skus: Collection<String>): List<ProductEntity>

    fun findAllByIsActiveTrueAndShowInCollectionsTrueAndCreatedGreaterThanEqualOrderByCreatedDesc(
        createdFrom: LocalDateTime,
        pageable: Pageable,
    ): List<ProductEntity>

    fun findAllByIsActiveTrueAndTitleContainingIgnoreCaseOrderByTitleAsc(
        title: String,
        pageable: Pageable,
    ): List<ProductEntity>

    @Transactional
    @Modifying
    @Query("update ProductEntity p set p.isActive = false")
    fun markAllProductsAsInactive(): Int

    @Transactional
    @Modifying
    @Query("update ProductEntity p set p.isActive = false where p.id=:productId")
    fun makeProductAsInactive(productId: Long)

    @Query(
        """
            select p
            from ProductEntity p
            join p.categories c
            where c.id = :categoryId
        """
    )
    fun findAllByCategoryId(@Param("categoryId") categoryId: Long): List<ProductEntity>

    @Query(
        """
            select p
            from ProductEntity p
            join p.categories c
            where c.id = :categoryId and p.isActive = true
        """
    )
    fun findAllByCategoryIdAndIsActiveTrue(@Param("categoryId") categoryId: Long): List<ProductEntity>
}
