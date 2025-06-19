package ru.foodbox.delivery.services.dto

data class DepartmentDto(
    val id: Long,
    val address: AddressDto,
    val workingHours: List<WorkingHourDto>,
    val currentWorkingHours: WorkingHourDto?,
    val isWorkingNow: Boolean,
)