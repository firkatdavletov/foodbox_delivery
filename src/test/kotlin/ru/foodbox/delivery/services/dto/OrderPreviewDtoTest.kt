package ru.foodbox.delivery.services.dto

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.foodbox.delivery.data.entities.OrderStatus
import java.math.BigDecimal

class OrderPreviewDtoTest {

    @Test
    fun `allows null delivery time`() {
        val dto = OrderPreviewDto(
            id = 1L,
            totalAmount = 149999,
            status = OrderStatus.PENDING,
            customerName = "Test User",
            companyName = null,
            deliveryTime = null,
        )

        assertNull(dto.deliveryTime)
    }
}
