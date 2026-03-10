package ru.foodbox.delivery.modules.cart.infrastructure.persistance.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.entities.AddressEntity
import ru.foodbox.delivery.data.entities.BaseAuditEntity
import ru.foodbox.delivery.data.entities.BaseEntity
import ru.foodbox.delivery.data.entities.DepartmentEntity
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "cart")
class CartEntity(
    @Id
    @Column(nullable = false)
    val id: UUID,
    @JoinColumn(name = "owner_id", nullable = false)
    var ownerId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    var ownerType: CartOwnerType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CartStatus,

    @OneToMany(
        mappedBy = "cart",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val items: MutableList<CartItemEntity> = mutableListOf(),

    @Column(name = "total_price")
    var totalPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)