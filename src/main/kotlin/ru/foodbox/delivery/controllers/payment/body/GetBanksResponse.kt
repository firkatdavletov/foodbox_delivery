package ru.foodbox.delivery.controllers.payment.body

import ru.foodbox.delivery.services.dto.BankDto

data class GetBanksResponse(
    val banks: List<BankDto>
)
