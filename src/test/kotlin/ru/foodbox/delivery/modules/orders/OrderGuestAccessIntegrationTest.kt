package ru.foodbox.delivery.modules.orders

import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.application.OrderStatusWorkflowDefaults
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderGuestAccessIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.execute("delete from payments")
        jdbcTemplate.execute("delete from order_status_history")
        jdbcTemplate.execute("delete from order_delivery_snapshots")
        jdbcTemplate.execute("delete from order_items")
        jdbcTemplate.execute("delete from orders")
    }

    @Test
    fun `guest can list own orders by x-device-id without bearer token`() {
        val ownOrder = createGuestOrder(installId = "device-123")
        createGuestOrder(installId = "device-999")

        mockMvc.perform(
            get("/api/v1/orders/my")
                .header("X-Device-Id", "device-123")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ownOrder.id.toString()))
            .andExpect(jsonPath("$[0].guestInstallId").value("device-123"))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
    }

    @Test
    fun `guest can get own order by x-device-id without bearer token`() {
        val ownOrder = createGuestOrder(installId = "device-123")

        mockMvc.perform(
            get("/api/v1/orders/{orderId}", ownOrder.id)
                .header("X-Device-Id", "device-123")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(ownOrder.id.toString()))
            .andExpect(jsonPath("$.guestInstallId").value("device-123"))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `guest current orders returns only non-final orders`() {
        val pendingOrder = createGuestOrder(installId = "device-123", statusCode = "PENDING")
        val confirmedOrder = createGuestOrder(installId = "device-123", statusCode = "CONFIRMED")
        createGuestOrder(installId = "device-123", statusCode = "COMPLETED")
        createGuestOrder(installId = "device-123", statusCode = "CANCELLED")
        createGuestOrder(installId = "device-999", statusCode = "PENDING")

        mockMvc.perform(
            get("/api/v1/orders/current")
                .header("X-Device-Id", "device-123")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(
                jsonPath(
                    "$[*].id",
                    containsInAnyOrder(pendingOrder.id.toString(), confirmedOrder.id.toString())
                )
            )
            .andExpect(jsonPath("$[*].status", containsInAnyOrder("PENDING", "CONFIRMED")))
    }

    @Test
    fun `guest cannot get another order by x-device-id`() {
        val ownOrder = createGuestOrder(installId = "device-123")

        mockMvc.perform(
            get("/api/v1/orders/{orderId}", ownOrder.id)
                .header("X-Device-Id", "device-999")
        )
            .andExpect(status().isForbidden)
    }

    private fun createGuestOrder(
        installId: String,
        statusCode: String = "PENDING",
    ): Order {
        val now = Instant.now()
        return orderRepository.save(
            Order(
                id = UUID.randomUUID(),
                orderNumber = "ORD-${System.nanoTime()}",
                customerType = OrderCustomerType.GUEST,
                userId = null,
                guestInstallId = installId,
                customerName = "Guest",
                customerPhone = "+79990000000",
                customerEmail = null,
                currentStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == statusCode },
                delivery = OrderDeliverySnapshot(
                    method = DeliveryMethodType.COURIER,
                    methodName = DeliveryMethodType.COURIER.displayName,
                    priceMinor = 450,
                    currency = "RUB",
                    zoneCode = "city",
                    zoneName = "City",
                    estimatedDays = 1,
                    pickupPointId = null,
                    pickupPointExternalId = null,
                    pickupPointName = null,
                    pickupPointAddress = null,
                    address = null,
                ),
                comment = null,
                items = listOf(
                    OrderItem(
                        id = UUID.randomUUID(),
                        productId = UUID.randomUUID(),
                        variantId = null,
                        sku = "ORDER-GUEST-1",
                        title = "Test product",
                        unit = ProductUnit.PIECE,
                        quantity = 1,
                        priceMinor = 1_000,
                        totalMinor = 1_000,
                    )
                ),
                subtotalMinor = 1_000,
                deliveryFeeMinor = 450,
                totalMinor = 1_450,
                statusChangedAt = now,
                createdAt = now,
                updatedAt = now,
            )
        )
    }
}
