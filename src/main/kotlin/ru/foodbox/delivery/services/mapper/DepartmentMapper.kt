package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.DepartmentEntity
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.dto.WorkingHourDto
import java.time.LocalDate
import java.time.LocalTime
import kotlin.compareTo

@Component
class DepartmentMapper(
    private val cityMapper: CityMapper,
    private val workingHourMapper: WorkingHourMapper,
) {
    fun toDto(
        entity: DepartmentEntity,
    ): DepartmentDto {
        val currentDate = LocalDate.now()
        val currentTime = LocalTime.now()
        val currentDayOfWeek = currentDate.dayOfWeek

        val workingHours = entity.workingHours.map { entity ->
            workingHourMapper.toDto(entity)
        }
        val currentWorkingHours = workingHours.find { it.dayOfWeek == currentDayOfWeek }

        val isWorking = if (currentWorkingHours != null) {
            currentTime > currentWorkingHours.openTime && currentTime < currentWorkingHours.closeTime
        } else {
            false
        }

        return DepartmentDto(
            id = entity.id!!,
            city = cityMapper.toDto(entity.cityEntity),
            name = entity.name,
            workingHours = workingHours,
            currentWorkingHours = currentWorkingHours,
            isWorkingNow = isWorking,
            latitude = entity.latitude,
            longitude = entity.longitude,
        )
    }
}