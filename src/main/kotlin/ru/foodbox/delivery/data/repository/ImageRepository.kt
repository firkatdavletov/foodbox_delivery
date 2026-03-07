package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.data.entities.ImageEntity
import ru.foodbox.delivery.services.model.UploadImageStatus
import java.time.LocalDateTime

interface ImageRepository: JpaRepository<ImageEntity, Long> {
    @Transactional
    @Modifying
    @Query(
        """
        update ImageEntity i
        set i.status = :failed
        where i.status = :uploading and i.created < :createdBefore
        """
    )
    fun markStaleUploadingAsFailed(
        @Param("uploading") uploading: UploadImageStatus,
        @Param("failed") failed: UploadImageStatus,
        @Param("createdBefore") createdBefore: LocalDateTime
    ): Int
}
