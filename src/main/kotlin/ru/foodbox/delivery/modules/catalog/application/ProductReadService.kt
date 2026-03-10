package ru.foodbox.delivery.modules.catalog.application

import ru.foodbox.delivery.modules.catalog.domain.ProductSnapshot
import java.util.UUID

interface ProductReadService {
    fun getActiveProductSnapshot(productId: UUID): ProductSnapshot?
}
