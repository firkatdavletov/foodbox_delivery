package ru.foodbox.delivery.modules.payments

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderStatus
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentsIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.execute("delete from payments")
        jdbcTemplate.execute("delete from order_delivery_snapshots")
        jdbcTemplate.execute("delete from order_items")
        jdbcTemplate.execute("delete from orders")
    }

    @Test
    fun `list available payment methods returns offline methods`() {
        val response = mockMvc.perform(get("/api/v1/payments/methods"))
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(response.response.contentAsByteArray)
        assertEquals(2, body.size())
        assertEquals("CASH", body[0]["code"].asText())
        assertEquals("CARD_ON_DELIVERY", body[1]["code"].asText())
        assertEquals(false, body[0]["isOnline"].asBoolean())
        assertEquals(true, body[0]["isActive"].asBoolean())
    }

    @Test
    fun `create payment stores record and updates order snapshot`() {
        val orderId = createGuestOrder(installId = "install-123")

        val response = mockMvc.perform(
            post("/api/v1/payments")
                .header("X-Install-Id", "install-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "orderId": "$orderId",
                      "paymentMethodCode": "CASH",
                      "details": "Оплата наличными курьеру"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(response.response.contentAsByteArray)
        assertEquals(orderId.toString(), body["orderId"].asText())
        assertEquals("CASH", body["paymentMethodCode"].asText())
        assertEquals("AWAITING_PAYMENT", body["status"].asText())
        assertEquals(1_450, body["amountMinor"].asLong())
        assertEquals("RUB", body["currency"].asText())
        assertEquals(false, body["isOnline"].asBoolean())

        val order = orderRepository.findById(orderId)
        assertNotNull(order)
        assertEquals("CASH", order.payment?.methodCode?.name)
        assertEquals("Наличными при получении", order.payment?.methodName)
    }

    @Test
    fun `creating second active payment for order returns conflict`() {
        val orderId = createGuestOrder(installId = "install-123")
        createPayment(orderId, installId = "install-123")

        mockMvc.perform(
            post("/api/v1/payments")
                .header("X-Install-Id", "install-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "orderId": "$orderId",
                      "paymentMethodCode": "CARD_ON_DELIVERY"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `payment details endpoint respects order ownership`() {
        val orderId = createGuestOrder(installId = "install-123")
        val payment = createPayment(orderId, installId = "install-123")

        mockMvc.perform(
            get("/api/v1/payments/{paymentId}", payment["id"].asText())
                .header("X-Install-Id", "install-999")
        )
            .andExpect(status().isForbidden)
    }

    private fun createPayment(orderId: UUID, installId: String): JsonNode {
        val response = mockMvc.perform(
            post("/api/v1/payments")
                .header("X-Install-Id", installId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "orderId": "$orderId",
                      "paymentMethodCode": "CASH"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(response.response.contentAsByteArray)
    }

    private fun createGuestOrder(installId: String): UUID {
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
                status = OrderStatus.PENDING,
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
                createdAt = now,
                updatedAt = now,
            )
        ).id
    }
}
