package ru.foodbox.delivery.modules.orders

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.application.OrderStatusChangeActor
import ru.foodbox.delivery.modules.orders.application.OrderStatusService
import ru.foodbox.delivery.modules.orders.application.OrderStatusWorkflowDefaults
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderStatusChangeSourceType
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderStatusAdminIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderStatusService: OrderStatusService

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.execute("delete from order_status_history")
        jdbcTemplate.execute("delete from order_items")
        jdbcTemplate.execute("delete from order_delivery_snapshots")
        jdbcTemplate.execute("delete from orders")
        jdbcTemplate.execute(
            "delete from order_status_transitions where id not in ('00000000-0000-0000-0000-000000000201'," +
                "'00000000-0000-0000-0000-000000000202'," +
                "'00000000-0000-0000-0000-000000000203'," +
                "'00000000-0000-0000-0000-000000000204')"
        )
        jdbcTemplate.execute(
            "delete from order_status_definitions where id not in ('00000000-0000-0000-0000-000000000101'," +
                "'00000000-0000-0000-0000-000000000102'," +
                "'00000000-0000-0000-0000-000000000103'," +
                "'00000000-0000-0000-0000-000000000104')"
        )
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin can create order status and retrieve it`() {
        val response = mockMvc.perform(
            post("/api/v1/admin/order-statuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "code": "READY_PICKUP",
                      "name": "Ready for pickup",
                      "stateType": "READY_FOR_PICKUP",
                      "color": "#111827",
                      "icon": "package",
                      "isActive": true,
                      "visibleToCustomer": true,
                      "notifyCustomer": true,
                      "notifyStaff": true,
                      "sortOrder": 30
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(response.response.contentAsByteArray)
        assertEquals("READY_PICKUP", body["code"].asText())

        mockMvc.perform(get("/api/v1/admin/order-statuses/{statusId}", body["id"].asText()))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin cannot create second initial status`() {
        mockMvc.perform(
            post("/api/v1/admin/order-statuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "code": "QUEUE",
                      "name": "Queue",
                      "stateType": "CREATED",
                      "isInitial": true,
                      "isActive": true,
                      "sortOrder": 5
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isConflict)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin changes order status and reads history`() {
        val orderId = createPendingOrder()

        mockMvc.perform(
            post("/api/v1/admin/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"statusCode":"CONFIRMED","comment":"Approved by admin"}""")
        )
            .andExpect(status().isOk)

        val historyResponse = mockMvc.perform(get("/api/v1/admin/orders/{orderId}/status-history", orderId))
            .andExpect(status().isOk)
            .andReturn()

        val history = objectMapper.readTree(historyResponse.response.contentAsByteArray)
        assertEquals(2, history.size())
        assertEquals("PENDING", history[0]["currentStatus"]["code"].asText())
        assertEquals("CONFIRMED", history[1]["currentStatus"]["code"].asText())
        assertEquals("Approved by admin", history[1]["comment"].asText())
    }

    private fun createPendingOrder(): UUID {
        val now = Instant.now()
        val order = orderRepository.save(
            Order(
                id = UUID.randomUUID(),
                orderNumber = "ORD-${System.nanoTime()}",
                customerType = OrderCustomerType.GUEST,
                userId = null,
                guestInstallId = "install-1",
                customerName = "Guest",
                customerPhone = "+79990000000",
                customerEmail = null,
                currentStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" },
                delivery = OrderDeliverySnapshot(
                    method = DeliveryMethodType.COURIER,
                    methodName = DeliveryMethodType.COURIER.displayName,
                    priceMinor = 500,
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
                        sku = "TEST-1",
                        title = "Test product",
                        unit = ProductUnit.PIECE,
                        quantity = 1,
                        priceMinor = 1_000,
                        totalMinor = 1_000,
                    )
                ),
                subtotalMinor = 1_000,
                deliveryFeeMinor = 500,
                totalMinor = 1_500,
                statusChangedAt = now,
                createdAt = now,
                updatedAt = now,
            )
        )

        orderStatusService.recordInitialStatus(
            order = order,
            actor = OrderStatusChangeActor(sourceType = OrderStatusChangeSourceType.SYSTEM),
        )
        return order.id
    }
}
