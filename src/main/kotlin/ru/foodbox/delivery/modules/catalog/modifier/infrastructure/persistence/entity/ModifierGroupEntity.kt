package ru.foodbox.delivery.modules.catalog.modifier.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "modifier_groups")
class ModifierGroupEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(nullable = false, unique = true, length = 128)
    var code: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(name = "min_selected", nullable = false)
    var minSelected: Int,

    @Column(name = "max_selected", nullable = false)
    var maxSelected: Int,

    @Column(name = "is_required", nullable = false)
    var isRequired: Boolean,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,
)
