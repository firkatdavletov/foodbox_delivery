package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.DepartmentEntity
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.dto.WorkingHourDto
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class DepartmentMapper(
    private val cityMapper: CityMapper,
    private val workingHourMapper: WorkingHourMapper,
) {
    fun toDto(
        entity: DepartmentEntity,
    ): DepartmentDto {
        val zoneId = ZoneId.of("Asia/Yekaterinburg") // UTC+5
        val now = ZonedDateTime.now(zoneId)

        val workingHours = entity.workingHours.map { entity ->
            workingHourMapper.toDto(entity)
        }

        val workingState = getCurrentWorkingState(
            now,
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
        now: ZonedDateTime,
        workingHours: List<WorkingHourDto>
    ): WorkingState {
        val day = now.dayOfWeek
        val time = now.toLocalTime()

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