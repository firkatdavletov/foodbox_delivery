package ru.foodbox.delivery.modules.cart.infrastructure.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.cart.domain.CartOwnerType
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "carts")
class CartEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 32)
    var ownerType: CartOwnerType,

    @Column(name = "owner_id", nullable = false, length = 255)
    var ownerId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: CartStatus,

    @Column(name = "total_price_minor", nullable = false)
    var totalPriceMinor: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @OneToMany(
        mappedBy = "cart",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    var items: MutableList<CartItemEntity> = mutableListOf(),
)
