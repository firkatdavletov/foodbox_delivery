package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.LocalTime

@Entity
@Table(name = "working_hours")
data class WorkingHourEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: DayOfWeek, // java.time.DayOfWeek

    @Column(name = "open_time", nullable = false)
    val openTime: LocalTime,

    @Column(name = "close_time", nullable = false)
    val closeTime: LocalTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    val department: DepartmentEntity
)