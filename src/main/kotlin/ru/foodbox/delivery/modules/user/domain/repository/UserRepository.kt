package ru.foodbox.delivery.modules.user.domain.repository

import ru.foodbox.delivery.modules.user.domain.User
import java.util.UUID

interface UserRepository {
    fun create(user: User): User
    fun findById(id: UUID): User?
}
