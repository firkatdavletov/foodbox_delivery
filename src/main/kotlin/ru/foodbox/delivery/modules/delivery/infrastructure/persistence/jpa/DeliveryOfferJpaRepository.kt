package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryOfferEntity
import java.util.UUID

interface DeliveryOfferJpaRepository : JpaRepository<DeliveryOfferEntity, UUID>
