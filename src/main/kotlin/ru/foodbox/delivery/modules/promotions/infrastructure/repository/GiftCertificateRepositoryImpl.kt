package ru.foodbox.delivery.modules.promotions.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificate
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificateTransaction
import ru.foodbox.delivery.modules.promotions.domain.repository.GiftCertificateRepository
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity.GiftCertificateEntity
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.entity.GiftCertificateTransactionEntity
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.jpa.GiftCertificateJpaRepository
import ru.foodbox.delivery.modules.promotions.infrastructure.persistence.jpa.GiftCertificateTransactionJpaRepository
import kotlin.jvm.optionals.getOrNull

@Repository
class GiftCertificateRepositoryImpl(
    private val giftCertificateJpaRepository: GiftCertificateJpaRepository,
    private val giftCertificateTransactionJpaRepository: GiftCertificateTransactionJpaRepository,
) : GiftCertificateRepository {

    override fun findByCodeForUpdate(code: String): GiftCertificate? {
        return giftCertificateJpaRepository.findByCodeForUpdate(code)?.toDomain()
    }

    override fun save(certificate: GiftCertificate): GiftCertificate {
        val existing = giftCertificateJpaRepository.findById(certificate.id).getOrNull()
        val entity = existing ?: GiftCertificateEntity(
            id = certificate.id,
            code = certificate.code,
            initialAmountMinor = certificate.initialAmountMinor,
            balanceMinor = certificate.balanceMinor,
            currency = certificate.currency,
            status = certificate.status,
            expiresAt = certificate.expiresAt,
            createdAt = certificate.createdAt,
            updatedAt = certificate.updatedAt,
        )

        entity.code = certificate.code
        entity.initialAmountMinor = certificate.initialAmountMinor
        entity.balanceMinor = certificate.balanceMinor
        entity.currency = certificate.currency
        entity.status = certificate.status
        entity.expiresAt = certificate.expiresAt
        entity.updatedAt = certificate.updatedAt

        return giftCertificateJpaRepository.save(entity).toDomain()
    }

    override fun saveTransaction(transaction: GiftCertificateTransaction): GiftCertificateTransaction {
        val entity = GiftCertificateTransactionEntity(
            id = transaction.id,
            giftCertificateId = transaction.giftCertificateId,
            orderId = transaction.orderId,
            type = transaction.type,
            amountMinor = transaction.amountMinor,
            createdAt = transaction.createdAt,
        )
        return giftCertificateTransactionJpaRepository.save(entity).toDomain()
    }

    private fun GiftCertificateEntity.toDomain(): GiftCertificate {
        return GiftCertificate(
            id = id,
            code = code,
            initialAmountMinor = initialAmountMinor,
            balanceMinor = balanceMinor,
            currency = currency,
            status = status,
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun GiftCertificateTransactionEntity.toDomain(): GiftCertificateTransaction {
        return GiftCertificateTransaction(
            id = id,
            giftCertificateId = giftCertificateId,
            orderId = orderId,
            type = type,
            amountMinor = amountMinor,
            createdAt = createdAt,
        )
    }
}
