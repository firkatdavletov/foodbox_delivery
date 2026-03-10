package ru.foodbox.delivery.modules.user.infrastructure.persistance.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.user.infrastructure.persistance.entity.UserEntity
import java.util.UUID

interface UserJpaRepository : JpaRepository<UserEntity, UUID>