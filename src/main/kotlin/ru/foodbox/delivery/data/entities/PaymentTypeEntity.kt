package ru.foodbox.delivery.data.entities

import jakarta.persistence.*

@Entity
@Table(name = "payment_types")
data class PaymentTypeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val key: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    val department: DepartmentEntity
)
