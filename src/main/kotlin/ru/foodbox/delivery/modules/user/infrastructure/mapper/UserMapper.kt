package ru.foodbox.delivery.modules.user.infrastructure.mapper

import ru.foodbox.delivery.modules.user.domain.User
import ru.foodbox.delivery.modules.user.infrastructure.persistance.entity.UserEntity

object UserMapper {
    fun map(entity: UserEntity): User {
        return User(
            id = entity.id,
            email = entity.email,
            phone = entity.phone,
            login = entity.login,
            company = entity.company,
        )
    }
}