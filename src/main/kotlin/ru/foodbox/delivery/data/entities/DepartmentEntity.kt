package ru.foodbox.delivery.data.entities

import jakarta.persistence.*

@Entity
@Table(name = "departments")
class DepartmentEntity(
    var name: String,

    @ManyToOne
    var cityEntity: CityEntity,

    var latitude: Double,

    var longitude: Double,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "can_deliver")
    var canDeliver: Boolean = true,

    @OneToMany(mappedBy = "department", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val workingHours: MutableList<WorkingHourEntity> = mutableListOf(),
) : BaseEntity<Long>() {
    fun addWorkingHours(block: DepartmentEntity.() -> WorkingHourEntity) {
        workingHours.add(block())
    }

    fun setWorkingHours(block: DepartmentEntity.() -> MutableSet<WorkingHourEntity>) {
        workingHours.clear()
        workingHours.addAll(block())
    }
}
