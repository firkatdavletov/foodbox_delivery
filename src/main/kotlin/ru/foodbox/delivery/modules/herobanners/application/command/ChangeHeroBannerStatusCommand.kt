package ru.foodbox.delivery.modules.herobanners.application.command

import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus

data class ChangeHeroBannerStatusCommand(
    val status: BannerStatus,
)
