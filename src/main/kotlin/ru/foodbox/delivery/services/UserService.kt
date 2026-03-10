package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.modules.user.domain.User
import ru.foodbox.delivery.services.mapper.UserMapper
import kotlin.jvm.optionals.getOrNull

@Service
class UserService(
    private val userRepository: UserRepository,
    private val authService: AuthService,
    private val userMapper: UserMapper,
) {
    fun getUser(id: Long): User {
        val entity = userRepository.findById(id).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")
        return userMapper.toDto(entity)
    }

    fun updateUser(id: Long, dto: User): User? {
        val user = userRepository.findById(id).getOrNull() ?: return null
        user.name = dto.name
        user.email = dto.email

        val savedUser = userRepository.save(user)
        return userMapper.toDto(savedUser)
    }

    fun deleteUser(userId: Long): Boolean {
        val user = userRepository.findById(userId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")
        userRepository.deleteById(user.id!!)
        return true
    }
}