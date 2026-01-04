package ru.foodbox.delivery.services.dto

data class DepartmentDto(
    val id: Long,
    val name: String,
    val city: CityDto,
    val latitude: Double,
    val longitude: Double,
    val workingHours: List<WorkingHourDto>,
    val currentWorkingHours: List<WorkingHourDto>?,
    val isWorkingNow: Boolean,
)