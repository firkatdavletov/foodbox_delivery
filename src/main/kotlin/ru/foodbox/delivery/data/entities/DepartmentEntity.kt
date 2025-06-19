package ru.foodbox.delivery.data.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "departments")
data class DepartmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "address")
    val address: AddressEntity,

    @OneToMany(mappedBy = "department", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val workingHours: List<WorkingHourEntity> = emptyList()
)
