package ru.foodbox.delivery.services.mapper

import org.springframework.stereotype.Component
import ru.foodbox.delivery.data.entities.BankEntity
import ru.foodbox.delivery.services.dto.BankDto

@Component
class BankMapper {
    fun toDto(entity: BankEntity): BankDto {
        return BankDto(
            bankName = entity.bankName,
            logoUrl = entity.logoUrl,
            packageName = entity.packageName,
            schema = entity.schema
        )
    }

    fun toDto(entities: List<BankEntity>) = entities.map { toDto(it) }
}