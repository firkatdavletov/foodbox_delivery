package ru.foodbox.delivery.controllers.departments.body

import ru.foodbox.delivery.services.dto.DepartmentDto

data class GetDepartmentsResponse(
    val departments: List<DepartmentDto>
)