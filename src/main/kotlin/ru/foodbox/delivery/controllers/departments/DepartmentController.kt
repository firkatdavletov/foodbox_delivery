package ru.foodbox.delivery.controllers.departments

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.services.DepartmentService
import ru.foodbox.delivery.controllers.departments.body.GetDepartmentsResponse

@RestController
@RequestMapping("/departments")
class DepartmentController(
    private val departmentService: DepartmentService,
) {
    @GetMapping
    fun getDepartments(): ResponseEntity<GetDepartmentsResponse> {
        val body = departmentService.getDepartments()
        val response = GetDepartmentsResponse(body)
        return ResponseEntity.ok(response)
    }
}