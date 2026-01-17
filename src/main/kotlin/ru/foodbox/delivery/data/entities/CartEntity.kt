package ru.foodbox.delivery.data.entities

import jakarta.persistence.*
import ru.foodbox.delivery.data.DeliveryType

@Entity
@Table(name = "cart")
class CartEntity(
    @JoinColumn(name = "device_id", unique = true)
    val deviceId: String,

    @ManyToOne()
    @JoinColumn(name = "department")
    var department: DepartmentEntity,

    @Column(name = "delivery_type", nullable = false)
    var deliveryType: DeliveryType,

    @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val items: MutableList<CartItemEntity> = mutableListOf(),

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "address")
    var deliveryAddress: AddressEntity?,

    @Column(name = "delivery_price")
    var deliveryPrice: Double,

    @Column(name = "free_delivery_price")
    var freeDeliveryPrice: Double?,

    @Column(name = "min_price_for_order")
    var minPriceForOrder: Double = 0.0,

    @Column(name = "discount")
    var discountPrice: Double = 0.0,

    @Column(name = "total_price")
    var totalPrice: Double = 0.0,

    var comment: String?,
) : BaseEntity<Long>() {

    fun addItem(block: CartEntity.() -> CartItemEntity) {
        items.add(block())
    }

    fun removeItem(block: CartEntity.() -> CartItemEntity) {
        items.remove(block())
    }

    fun setItems(block: CartEntity.() -> MutableSet<CartItemEntity>) {
        items.clear()
        items.addAll(block())
    }

    fun updateTotalPrice() {
        totalPrice = if (items.isEmpty()) {
            0.0
        } else {
            items.sumOf { it.product.price * it.quantity } + deliveryPrice
        }
    }
}