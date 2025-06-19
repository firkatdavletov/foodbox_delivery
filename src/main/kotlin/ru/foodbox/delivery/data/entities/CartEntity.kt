package ru.foodbox.delivery.data.entities

import jakarta.persistence.*
import ru.foodbox.delivery.data.DeliveryType

@Entity
@Table(name = "cart")
data class CartEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne()
    @JoinColumn(name = "user_id")
    val user: UserEntity,

    @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val items: List<CartItemEntity> = emptyList(),

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    val deliveryType: DeliveryType = DeliveryType.PICKUP,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "address")
    val deliveryAddress: AddressEntity? = null,

    @Column(name = "delivery_price")
    val deliveryPrice: Double = 0.0,

    @Column(name = "total_price")
    val totalPrice: Double = 0.0,

    @ManyToOne()
    @JoinColumn(name = "department")
    val department: DepartmentEntity? = null
)