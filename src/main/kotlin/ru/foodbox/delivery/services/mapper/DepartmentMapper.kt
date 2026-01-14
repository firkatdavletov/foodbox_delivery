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

        val workingState = getCurrentWorkingState(
            currentDayOfWeek,
            currentTime,
            workingHours
        )

        return DepartmentDto(
            id = entity.id!!,
            city = cityMapper.toDto(entity.cityEntity),
            name = entity.name,
            currentWorkingHours = workingState.currentWorkingHours,
            isWorkingNow = workingState.isOpen,
            latitude = entity.latitude,
            longitude = entity.longitude,
        )
    }

    fun getCurrentWorkingState(
        day: DayOfWeek,
        time: LocalTime,
        workingHours: List<WorkingHourDto>
    ): WorkingState {

        workingHours.forEach { wh ->
            val crossesMidnight = wh.openTime >= wh.closeTime

            val isOpenNow = if (!crossesMidnight) {
                day == wh.dayOfWeek &&
                        time >= wh.openTime &&
                        time < wh.closeTime
            } else {
                (day == wh.dayOfWeek && time >= wh.openTime) ||
                        (day == wh.dayOfWeek.next() && time < wh.closeTime)
            }

            if (isOpenNow) {
                return WorkingState(
                    isOpen = true,
                    currentWorkingHours = wh
                )
            }
        }

        return WorkingState(
            isOpen = false,
            currentWorkingHours = null
        )
    }


    private fun DayOfWeek.next(): DayOfWeek =
        DayOfWeek.of((this.value % 7) + 1)

    data class WorkingState(
        val isOpen: Boolean,
        val currentWorkingHours: WorkingHourDto?
    )
}