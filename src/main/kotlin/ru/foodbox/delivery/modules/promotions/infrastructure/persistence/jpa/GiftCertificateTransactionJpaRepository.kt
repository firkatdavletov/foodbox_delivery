package ru.foodbox.delivery.modules.promotions.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity.GiftCertificateTransactionEntity
import java.util.UUID

interface GiftCertificateTransactionJpaRepository : JpaRepository<GiftCertificateTransactionEntity, UUID>
