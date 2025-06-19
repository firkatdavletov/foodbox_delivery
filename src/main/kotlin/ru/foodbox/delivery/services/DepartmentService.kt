package ru.foodbox.delivery.services

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import ru.foodbox.delivery.controllers.departments.body.GetDepartmentsResponse
import ru.foodbox.delivery.data.repository.DepartmentRepository
import ru.foodbox.delivery.services.dto.WorkingHourDto
import ru.foodbox.delivery.services.mapper.AddressMapper
import ru.foodbox.delivery.services.mapper.DepartmentMapper
import ru.foodbox.delivery.services.mapper.WorkingHourMapper
import java.time.LocalDate
import java.time.LocalTime

@Service
class DepartmentService(
    private val departmentRepository: DepartmentRepository,
    private val departmentMapper: DepartmentMapper,
    private val addressMapper: AddressMapper,
    private val workingHourMapper: WorkingHourMapper,
) {
    fun getDepartments(): GetDepartmentsResponse {
        val entities = departmentRepository.findAll()
        val currentDate = LocalDate.now()
        val currentTime = LocalTime.now()
        val currentDayOfWeek = currentDate.dayOfWeek

        return GetDepartmentsResponse(
            departments = entities.map { department ->
                val addressDto = addressMapper.toDto(department.address)
                val workingHours = department.workingHours.map { entity ->
                    workingHourMapper.toDto(entity)
                }
                val currentWorkingHours = workingHours.find { it.dayOfWeek == currentDayOfWeek }

                val isWorking = if (currentWorkingHours != null) {
                    currentTime > currentWorkingHours.openTime && currentTime < currentWorkingHours.closeTime
                } else {
                    false
                }

                departmentMapper.toDto(
                    entity = department,
                    addressDto = addressDto,
                    workingHours = workingHours,
                    currentWorkingHours = currentWorkingHours,
                    isWorkingNow = isWorking
                )
            }
        )
    }

    fun updateWorkingHours(departmentId: Long, newDtos: List<WorkingHourDto>) {
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { EntityNotFoundException("Department not found") }

        val newWorkingHours = newDtos.map { dto ->
            workingHourMapper.toEntity(dto, department)
        }

        val updatedDepartment = department.copy(workingHours = newWorkingHours)

        departmentRepository.save(updatedDepartment)
    }
}