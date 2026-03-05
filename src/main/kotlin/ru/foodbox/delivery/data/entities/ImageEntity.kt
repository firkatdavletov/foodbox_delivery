package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import ru.foodbox.delivery.services.model.UploadImageStatus
import java.time.LocalDateTime

@Entity
@Table(name = "images")
class ImageEntity(
    @Column(name = "storage_key", nullable = false, unique = true)
    var storageKey: String,

    @Column(nullable = false)
    var variant: String,

    @Column(nullable = false)
    var width: Int,

    @Column(nullable = false)
    var height: Int,

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long,

    @Column(nullable = false)
    var mime: String,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UploadImageStatus
) : BaseAuditEntity<Long>()
