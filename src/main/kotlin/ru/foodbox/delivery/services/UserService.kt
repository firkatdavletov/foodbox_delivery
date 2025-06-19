package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.data.entities.UserEntity
import ru.foodbox.delivery.services.dto.UserDto
import ru.foodbox.delivery.services.mapper.UserMapper
import kotlin.jvm.optionals.getOrNull

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
) {
    fun getUser(id: Long): UserDto {
        val entity = userRepository.findById(id).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")
        return userMapper.toDto(entity)
    }

    fun updateUser(id: Long, dto: UserDto): UserDto {
        val entity = userRepository.findById(id).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")
        val updatedUser = entity.copy(
            name = dto.name,
            phone = dto.phone
        )

        val savedUser = userRepository.save(updatedUser)
        return userMapper.toDto(savedUser)
    }

    fun deleteUser(userId: Long): Boolean {
        val entity = userRepository.findById(userId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")
        userRepository.deleteById(entity.id)
        return true
    }
}