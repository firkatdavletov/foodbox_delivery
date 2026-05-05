package ru.foodbox.delivery.modules.promotions.domain.repository

import ru.foodbox.delivery.modules.promotions.domain.GiftCertificate
import ru.foodbox.delivery.modules.promotions.domain.GiftCertificateTransaction

interface GiftCertificateRepository {
    fun findByCodeForUpdate(code: String): GiftCertificate?
    fun save(certificate: GiftCertificate): GiftCertificate
    fun saveTransaction(transaction: GiftCertificateTransaction): GiftCertificateTransaction
}
