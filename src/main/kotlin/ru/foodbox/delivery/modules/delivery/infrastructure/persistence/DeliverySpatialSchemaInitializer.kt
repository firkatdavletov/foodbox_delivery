package ru.foodbox.delivery.modules.delivery.infrastructure.persistence

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DeliverySpatialSchemaInitializer(
    private val deliveryZoneGeometryProjector: DeliveryZoneGeometryProjector,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        deliveryZoneGeometryProjector.initializeSchema()
        deliveryZoneGeometryProjector.rebuildEffectiveGeometries()
    }
}
