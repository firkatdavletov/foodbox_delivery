package ru.foodbox.delivery.services

import org.springframework.stereotype.Service
import ru.foodbox.delivery.data.cloudpayments_client.CloudPaymentsClient
import ru.foodbox.delivery.data.entities.BankEntity
import ru.foodbox.delivery.data.entities.BankInfoVersionEntity
import ru.foodbox.delivery.data.repository.BankInfoVersionRepository
import ru.foodbox.delivery.data.repository.BankRepository
import ru.foodbox.delivery.services.dto.BankDto
import ru.foodbox.delivery.services.mapper.BankMapper

@Service
class BankService(
    private val client: CloudPaymentsClient,
    private val bankRepository: BankRepository,
    private val bankInfoVersionRepository: BankInfoVersionRepository,
    private val bankMapper: BankMapper,
) {
    fun getQrBanks(): List<BankDto> {
        val banks = bankRepository.findAllByCanStoreToken(false)

        if (banks.isEmpty()) {
            updateBankDictionary("qr")
            val banks = bankRepository.findAllByCanStoreToken(false)
            return bankMapper.toDto(banks)
        }

        return bankMapper.toDto(banks)
    }

    fun getSubBanks(): List<BankDto> {
        val banks = bankRepository.findAllByCanStoreToken(true)

        if (banks.isEmpty()) {
            updateBankDictionary("sub")
            val banks = bankRepository.findAllByCanStoreToken(false)
            return bankMapper.toDto(banks)
        }

        return bankMapper.toDto(banks)
    }

    fun updateBankDictionary(key: String) {
        val currentDictionaryVersion = bankInfoVersionRepository.findFirstByKey(key)

        val remoteDictionary = when (key) {
            "sub" -> {
                client.getSubBanks()
            }
            "qr" -> {
                client.getQrBanks()
            }

            else -> {
                throw RuntimeException("unknown key")
            }
        }

        if (currentDictionaryVersion != null && currentDictionaryVersion.version == remoteDictionary.version) {
            return
        } else {
            val entities = remoteDictionary.dictionary.map {
                BankEntity(
                    bankName = it.bankName,
                    logoUrl = it.logoUrl,
                    schema = it.schema,
                    packageName = it.packageName,
                    canStoreToken = key == "sub"
                )
            }
            val newVersion = if (currentDictionaryVersion == null) {
                BankInfoVersionEntity(key = key, version = remoteDictionary.version)
            } else {
                currentDictionaryVersion.version = remoteDictionary.version
                currentDictionaryVersion
            }

            bankRepository.saveAll(entities)
            bankInfoVersionRepository.save(newVersion)
        }
    }
}