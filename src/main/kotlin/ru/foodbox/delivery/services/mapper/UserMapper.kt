package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.UserEntity
import ru.foodbox.delivery.services.dto.UserDto

@Component
class UserMapper {
    fun toDto(entity: UserEntity) = UserDto(
        phone = entity.phone,
        name = entity.name,
    )
}