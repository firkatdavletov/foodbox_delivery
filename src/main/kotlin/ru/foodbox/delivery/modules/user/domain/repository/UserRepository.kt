package ru.foodbox.delivery.modules.user.domain.repository

import ru.foodbox.delivery.modules.user.domain.User

interface UserRepository {
    fun create(user: User): User
}