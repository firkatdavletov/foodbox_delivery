package ru.foodbox.delivery.modules.herobanners.api.dto

import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus

data class ChangeHeroBannerStatusRequest(
    @field:NotNull
    val status: BannerStatus,
)
