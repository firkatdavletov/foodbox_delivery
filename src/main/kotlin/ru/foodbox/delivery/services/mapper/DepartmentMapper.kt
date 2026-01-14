package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.DepartmentEntity
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.dto.WorkingHourDto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

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

        val currentWorkingHours = workingHours.firstOrNull { it.dayOfWeek == currentDayOfWeek }

        return DepartmentDto(
            id = entity.id!!,
            city = cityMapper.toDto(entity.cityEntity),
            name = entity.name,
            currentWorkingHours = currentWorkingHours,
            isWorkingNow = isOpen(currentDayOfWeek, currentTime, workingHours),
            latitude = entity.latitude,
            longitude = entity.longitude,
        )
    }

    fun isOpen(
        day: DayOfWeek,
        time: LocalTime,
        workingHours: List<WorkingHourDto>
    ): Boolean {
        workingHours.forEach { wh ->
            val crossesMidnight = wh.openTime >= wh.closeTime

            val isOpen = if (!crossesMidnight) {
                day == wh.dayOfWeek &&
                        time >= wh.openTime &&
                        time < wh.closeTime
            } else {
                (day == wh.dayOfWeek && time >= wh.openTime) ||
                        (day == wh.dayOfWeek.plus(1) && time < wh.closeTime)
            }

            if (isOpen) {
                return true
            }
        }
        return false
    }
}