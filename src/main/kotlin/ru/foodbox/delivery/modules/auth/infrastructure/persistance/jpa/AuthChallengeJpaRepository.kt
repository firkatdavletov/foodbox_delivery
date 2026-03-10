package ru.foodbox.delivery.modules.auth.infrastructure.persistance.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity.AuthChallengeEntity
import java.util.UUID

interface AuthChallengeJpaRepository : JpaRepository<AuthChallengeEntity, UUID>