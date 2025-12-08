package ru.foodbox.delivery.services

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import ru.foodbox.delivery.data.repository.DepartmentRepository
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.dto.WorkingHourDto
import ru.foodbox.delivery.services.mapper.DepartmentMapper
import ru.foodbox.delivery.services.mapper.WorkingHourMapper

@Service
class DepartmentService(
    private val departmentRepository: DepartmentRepository,
    private val departmentMapper: DepartmentMapper,
    private val workingHourMapper: WorkingHourMapper,
) {
    fun getDepartments(): List<DepartmentDto> {
        val entities = departmentRepository.findAll()


        val departments = entities.map { department ->
            departmentMapper.toDto(
                entity = department,
            )
        }

        return departments
    }

    fun updateWorkingHours(departmentId: Long, newDtos: List<WorkingHourDto>) {
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { EntityNotFoundException("Department not found") }

        val newWorkingHours = newDtos.map { dto ->
            workingHourMapper.toEntity(dto, department)
        }.toMutableSet()

        department.setWorkingHours { newWorkingHours }

        departmentRepository.save(department)
    }
}