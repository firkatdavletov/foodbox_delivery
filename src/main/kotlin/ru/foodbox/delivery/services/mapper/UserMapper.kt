package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.user.infrastructure.persistance.entity.UserEntity
import ru.foodbox.delivery.modules.user.domain.User

@Component
class UserMapper {
    fun toDto(entity: UserEntity) = User(
        phone = entity.phone,
        name = entity.name,
        email = entity.email,
        company = entity.company,
    )
}