package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.AddressEntity
import ru.foodbox.delivery.data.entities.DepartmentEntity
import ru.foodbox.delivery.data.entities.WorkingHourEntity
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.dto.WorkingHourDto

@Component
class DepartmentMapper {
    fun toEntity(dto: DepartmentDto, addressEntity: AddressEntity, workingHours: List<WorkingHourEntity>) =
        DepartmentEntity(
            id = dto.id,
            address = addressEntity,
            workingHours = workingHours
        )

    fun toDto(
        entity: DepartmentEntity,
        addressDto: AddressDto,
        workingHours: List<WorkingHourDto>,
        currentWorkingHours: WorkingHourDto?,
        isWorkingNow: Boolean
    ) = DepartmentDto(
        id = entity.id,
        address = addressDto,
        workingHours = workingHours,
        currentWorkingHours = currentWorkingHours,
        isWorkingNow = isWorkingNow
    )
}