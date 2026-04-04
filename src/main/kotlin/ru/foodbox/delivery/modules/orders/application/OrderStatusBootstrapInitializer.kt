package ru.foodbox.delivery.modules.orders.application

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class OrderStatusBootstrapInitializer(
    private val orderStatusAdminService: OrderStatusAdminService,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        orderStatusAdminService.bootstrapDefaultsIfNeeded()
    }
}
