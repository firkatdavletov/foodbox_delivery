package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.DepartmentEntity
import ru.foodbox.delivery.data.entities.WorkingHourEntity
import ru.foodbox.delivery.services.dto.WorkingHourDto

@Component
class WorkingHourMapper {

    fun toDto(entity: WorkingHourEntity): WorkingHourDto {
        return WorkingHourDto(
            dayOfWeek = entity.dayOfWeek,
            openTime = entity.openTime,
            closeTime = entity.closeTime,
        )
    }
}