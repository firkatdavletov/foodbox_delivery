package ru.foodbox.delivery.data.entities

import jakarta.persistence.*
import ru.foodbox.delivery.data.DeliveryType
import java.math.BigDecimal

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
    var deliveryPrice: BigDecimal,

    @Column(name = "free_delivery_price")
    var freeDeliveryPrice: BigDecimal?,

    @Column(name = "min_price_for_order")
    var minPriceForOrder: BigDecimal,

    @Column(name = "discount")
    var discountPrice: BigDecimal,

    @Column(name = "total_price")
    var totalPrice: BigDecimal,

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
        val itemsPrice = items.sumOf { it.product.price * BigDecimal(it.quantity) }
        val freeDeliveryPrice = freeDeliveryPrice
        val totalDeliveryPrice = if (freeDeliveryPrice != null && itemsPrice >= freeDeliveryPrice) {
            BigDecimal.ZERO
        } else {
            deliveryPrice
        }
        totalPrice = if (items.isEmpty()) {
            BigDecimal(0.0)
        } else {
            itemsPrice + totalDeliveryPrice
        }
    }
}