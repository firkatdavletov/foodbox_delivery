package ru.foodbox.delivery.modules.orders

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.application.OrderStatusWorkflowDefaults
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderPaymentSnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOrderListIntegrationTest {

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
        jdbcTemplate.execute("delete from order_status_history")
        jdbcTemplate.execute("delete from order_item_modifiers")
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
    fun `default request returns active queue page`() {
        val now = Instant.parse("2026-04-13T12:00:00Z")
        val onHoldStatus = insertStatus("ON_HOLD_LIST", "On hold", OrderStateType.ON_HOLD, 50, now)
        val completedStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "COMPLETED" }
        val canceledStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "CANCELLED" }

        val firstId = createOrder(
            orderNumber = "LIST-001",
            status = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" },
            createdAt = now.minusSeconds(300),
            customerName = "Alice",
        )
        val secondId = createOrder(
            orderNumber = "LIST-002",
            status = OrderStatusWorkflowDefaults.statuses.first { it.code == "CONFIRMED" },
            createdAt = now.minusSeconds(120),
            customerName = "Bob",
        )
        createOrder(
            orderNumber = "LIST-003",
            status = onHoldStatus,
            createdAt = now.minusSeconds(60),
            customerName = "Charlie",
        )
        createOrder(
            orderNumber = "LIST-004",
            status = completedStatus,
            createdAt = now.minusSeconds(30),
        )
        createOrder(
            orderNumber = "LIST-005",
            status = canceledStatus,
            createdAt = now,
        )

        insertPayment(firstId, PaymentStatus.AWAITING_PAYMENT, now.minusSeconds(200))
        insertPayment(secondId, PaymentStatus.SUCCEEDED, now.minusSeconds(100))

        val body = performList()

        assertEquals(3, body["items"].size())
        assertEquals("LIST-003", body["items"][0]["orderNumber"].asText())
        assertEquals("LIST-002", body["items"][1]["orderNumber"].asText())
        assertEquals("LIST-001", body["items"][2]["orderNumber"].asText())
        assertEquals("AWAITING_PAYMENT", body["items"][2]["paymentStatus"]["code"].asText())
        assertEquals("AWAITING_PAYMENT", body["items"][2]["paymentStatus"]["name"].asText())
        assertEquals("CARD_ONLINE", body["items"][2]["payment"]["code"].asText())
        assertNull(body["items"][2]["manager"].takeIf { !it.isNull })
        assertNull(body["items"][2]["source"].takeIf { !it.isNull })
        assertEquals(0, body["items"][2]["tags"].size())
        assertEquals(1, body["meta"]["page"].asInt())
        assertEquals(25, body["meta"]["pageSize"].asInt())
        assertEquals(3L, body["meta"]["totalItems"].asLong())
        assertEquals(1, body["meta"]["totalPages"].asInt())
        assertEquals("createdAt", body["meta"]["sortBy"].asText())
        assertEquals("desc", body["meta"]["sortDirection"].asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `pagination applies page and pageSize`() {
        val now = Instant.parse("2026-04-13T10:00:00Z")
        val pendingStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }

        repeat(30) { index ->
            createOrder(
                orderNumber = "PAGE-${index + 1}",
                status = pendingStatus,
                createdAt = now.plusSeconds(index.toLong()),
            )
        }

        val body = performList("page" to "2", "pageSize" to "25")

        assertEquals(5, body["items"].size())
        assertEquals("PAGE-5", body["items"][0]["orderNumber"].asText())
        assertEquals("PAGE-1", body["items"][4]["orderNumber"].asText())
        assertEquals(2, body["meta"]["page"].asInt())
        assertEquals(25, body["meta"]["pageSize"].asInt())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `status filter returns only matching orders`() {
        val now = Instant.parse("2026-04-13T09:00:00Z")
        createOrder(
            orderNumber = "STATUS-1",
            status = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" },
            createdAt = now,
        )
        createOrder(
            orderNumber = "STATUS-2",
            status = OrderStatusWorkflowDefaults.statuses.first { it.code == "CONFIRMED" },
            createdAt = now.plusSeconds(10),
        )

        val body = performList("statusCodes" to "confirmed")

        assertEquals(1, body["items"].size())
        assertEquals("STATUS-2", body["items"][0]["orderNumber"].asText())
        assertEquals("CONFIRMED", body["items"][0]["currentStatus"]["code"].asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `delivery filter returns only matching orders`() {
        val now = Instant.parse("2026-04-13T08:00:00Z")
        val pendingStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }

        createOrder(
            orderNumber = "DELIVERY-1",
            status = pendingStatus,
            createdAt = now,
            deliveryMethod = DeliveryMethodType.COURIER,
        )
        createOrder(
            orderNumber = "DELIVERY-2",
            status = pendingStatus,
            createdAt = now.plusSeconds(10),
            deliveryMethod = DeliveryMethodType.PICKUP,
        )

        val body = performList("deliveryMethods" to "pickup")

        assertEquals(1, body["items"].size())
        assertEquals("DELIVERY-2", body["items"][0]["orderNumber"].asText())
        assertEquals("PICKUP", body["items"][0]["deliveryMethod"].asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `date filter applies inclusive boundaries`() {
        val pendingStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }

        createOrder(
            orderNumber = "DATE-1",
            status = pendingStatus,
            createdAt = Instant.parse("2026-04-10T23:59:59Z"),
        )
        createOrder(
            orderNumber = "DATE-2",
            status = pendingStatus,
            createdAt = Instant.parse("2026-04-11T12:00:00Z"),
        )
        createOrder(
            orderNumber = "DATE-3",
            status = pendingStatus,
            createdAt = Instant.parse("2026-04-12T00:00:00Z"),
        )

        val body = performList("createdFrom" to "2026-04-11", "createdTo" to "2026-04-11")

        assertEquals(1, body["items"].size())
        assertEquals("DATE-2", body["items"][0]["orderNumber"].asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `scope filters split queue into new in work and problematic`() {
        val now = Instant.parse("2026-04-13T11:00:00Z")
        val createdStatus = insertStatus("CREATED_QUEUE", "Created", OrderStateType.CREATED, 5, now)
        val preparingStatus = insertStatus("PREPARING_QUEUE", "Preparing", OrderStateType.PREPARING, 30, now)
        val onHoldStatus = insertStatus("ON_HOLD_QUEUE", "On hold", OrderStateType.ON_HOLD, 50, now)

        createOrder(
            orderNumber = "SCOPE-NEW-1",
            status = createdStatus,
            createdAt = now.minusSeconds(30),
        )
        createOrder(
            orderNumber = "SCOPE-NEW-2",
            status = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" },
            createdAt = now.minusSeconds(20),
        )
        createOrder(
            orderNumber = "SCOPE-WORK-1",
            status = OrderStatusWorkflowDefaults.statuses.first { it.code == "CONFIRMED" },
            createdAt = now.minusSeconds(10),
        )
        createOrder(
            orderNumber = "SCOPE-WORK-2",
            status = preparingStatus,
            createdAt = now.minusSeconds(5),
        )
        createOrder(
            orderNumber = "SCOPE-PROBLEM",
            status = onHoldStatus,
            createdAt = now,
        )

        val newBody = performList("scope" to "new")
        val inWorkBody = performList("scope" to "in_work")
        val problematicBody = performList("scope" to "problematic")

        assertEquals(2, newBody["items"].size())
        assertEquals(
            setOf("SCOPE-NEW-1", "SCOPE-NEW-2"),
            newBody["items"].map { it["orderNumber"].asText() }.toSet(),
        )
        assertEquals(2, inWorkBody["items"].size())
        assertEquals(
            setOf("SCOPE-WORK-1", "SCOPE-WORK-2"),
            inWorkBody["items"].map { it["orderNumber"].asText() }.toSet(),
        )
        assertEquals(1, problematicBody["items"].size())
        assertEquals("SCOPE-PROBLEM", problematicBody["items"][0]["orderNumber"].asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `sorting works for createdAt and totalMinor`() {
        val now = Instant.parse("2026-04-13T06:00:00Z")
        val pendingStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }

        createOrder(
            orderNumber = "SORT-1",
            status = pendingStatus,
            createdAt = now.plusSeconds(100),
            totalMinor = 3_000L,
        )
        createOrder(
            orderNumber = "SORT-2",
            status = pendingStatus,
            createdAt = now.plusSeconds(200),
            totalMinor = 1_000L,
        )
        createOrder(
            orderNumber = "SORT-3",
            status = pendingStatus,
            createdAt = now.plusSeconds(300),
            totalMinor = 2_000L,
        )

        val createdAtBody = performList("sortBy" to "createdAt", "sortDirection" to "asc")
        val totalMinorBody = performList("sortBy" to "totalMinor", "sortDirection" to "asc")

        assertEquals(listOf("SORT-1", "SORT-2", "SORT-3"), createdAtBody["items"].map { it["orderNumber"].asText() })
        assertEquals(listOf("SORT-2", "SORT-3", "SORT-1"), totalMinorBody["items"].map { it["orderNumber"].asText() })
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `search finds order by partial order number`() {
        val now = Instant.parse("2026-04-13T07:00:00Z")
        val pendingStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }

        createOrder(
            orderNumber = "SEARCH-1001",
            status = pendingStatus,
            createdAt = now,
        )
        createOrder(
            orderNumber = "SEARCH-2002",
            status = pendingStatus,
            createdAt = now.plusSeconds(10),
        )

        val body = performList("search" to "1001")

        assertEquals(1, body["items"].size())
        assertEquals("SEARCH-1001", body["items"][0]["orderNumber"].asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `combined filters and pagination return ready slice`() {
        val now = Instant.parse("2026-04-11T10:00:00Z")
        val confirmedStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "CONFIRMED" }
        val pendingStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }

        repeat(30) { index ->
            createOrder(
                orderNumber = "MATCH-${index + 1}",
                status = confirmedStatus,
                createdAt = now.plusSeconds(index.toLong()),
                deliveryMethod = DeliveryMethodType.COURIER,
            )
        }
        repeat(10) { index ->
            createOrder(
                orderNumber = "SKIP-${index + 1}",
                status = pendingStatus,
                createdAt = now.plusSeconds(10_000 + index.toLong()),
                deliveryMethod = DeliveryMethodType.PICKUP,
            )
        }

        val body = performList(
            "statusCodes" to "CONFIRMED",
            "deliveryMethods" to "courier",
            "createdFrom" to "2026-04-11",
            "createdTo" to "2026-04-11",
            "page" to "2",
            "pageSize" to "25",
            "sortBy" to "createdAt",
            "sortDirection" to "asc",
        )

        assertEquals(5, body["items"].size())
        assertEquals("MATCH-26", body["items"][0]["orderNumber"].asText())
        assertEquals("MATCH-30", body["items"][4]["orderNumber"].asText())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `meta returns correct totalItems and totalPages`() {
        val now = Instant.parse("2026-04-13T05:00:00Z")
        val pendingStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }

        repeat(51) { index ->
            createOrder(
                orderNumber = "META-${index + 1}",
                status = pendingStatus,
                createdAt = now.plusSeconds(index.toLong()),
            )
        }

        val body = performList("page" to "1", "pageSize" to "25")

        assertEquals(51L, body["meta"]["totalItems"].asLong())
        assertEquals(3, body["meta"]["totalPages"].asInt())
        assertEquals(25, body["items"].size())
    }

    private fun performList(vararg params: Pair<String, String>): JsonNode {
        val request = get("/api/v1/admin/orders")
        params.forEach { (name, value) -> request.param(name, value) }
        val response = mockMvc.perform(request)
            .andExpect(status().isOk)
            .andReturn()
        return objectMapper.readTree(response.response.contentAsByteArray)
    }

    private fun insertStatus(
        code: String,
        name: String,
        stateType: OrderStateType,
        sortOrder: Int,
        now: Instant,
    ): OrderStatusDefinition {
        val statusId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            insert into order_status_definitions (
                id, code, name, description, state_type, color, icon, is_initial, is_final, is_cancellable,
                is_active, visible_to_customer, notify_customer, notify_staff, sort_order, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            statusId,
            code,
            name,
            "$name description",
            stateType.name,
            "#111827",
            "package",
            false,
            false,
            stateType != OrderStateType.ON_HOLD,
            true,
            true,
            true,
            true,
            sortOrder,
            now,
            now,
        )
        return OrderStatusDefinition(
            id = statusId,
            code = code,
            name = name,
            description = "$name description",
            stateType = stateType,
            color = "#111827",
            icon = "package",
            isInitial = false,
            isFinal = false,
            isCancellable = stateType != OrderStateType.ON_HOLD,
            isActive = true,
            visibleToCustomer = true,
            notifyCustomer = true,
            notifyStaff = true,
            sortOrder = sortOrder,
        )
    }

    private fun createOrder(
        orderNumber: String,
        status: OrderStatusDefinition,
        createdAt: Instant,
        deliveryMethod: DeliveryMethodType = DeliveryMethodType.COURIER,
        totalMinor: Long = 1_500L,
        customerName: String? = "List Customer",
        customerPhone: String? = "+79990000000",
        customerEmail: String? = "list@example.com",
    ): UUID {
        val subtotalMinor = totalMinor - 500L
        return orderRepository.save(
            Order(
                id = UUID.randomUUID(),
                orderNumber = orderNumber,
                customerType = OrderCustomerType.GUEST,
                userId = null,
                guestInstallId = "list-install",
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                currentStatus = status,
                delivery = OrderDeliverySnapshot(
                    method = deliveryMethod,
                    methodName = deliveryMethod.displayName,
                    priceMinor = 500L,
                    currency = "RUB",
                    zoneCode = "city",
                    zoneName = "City",
                    estimatedDays = 1,
                    pickupPointId = null,
                    pickupPointExternalId = null,
                    pickupPointName = if (deliveryMethod.requiresPickupPoint) "Pickup point" else null,
                    pickupPointAddress = if (deliveryMethod.requiresPickupPoint) "Pickup address" else null,
                    address = if (deliveryMethod.requiresAddress) {
                        val date = LocalDate.ofInstant(createdAt, ZoneOffset.UTC)
                        ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress(
                            country = "RU",
                            region = "Sverdlovsk",
                            city = "Yekaterinburg",
                            street = "Lenina",
                            house = date.dayOfMonth.toString(),
                            apartment = "10",
                            postalCode = "620000",
                            entrance = "1",
                            floor = "2",
                            intercom = "10",
                        )
                    } else {
                        null
                    },
                ),
                comment = null,
                items = listOf(
                    OrderItem(
                        id = UUID.randomUUID(),
                        productId = UUID.randomUUID(),
                        variantId = null,
                        sku = "SKU-$orderNumber",
                        title = "List product",
                        unit = ProductUnit.PIECE,
                        quantity = 1,
                        priceMinor = subtotalMinor,
                        totalMinor = subtotalMinor,
                    )
                ),
                subtotalMinor = subtotalMinor,
                deliveryFeeMinor = 500L,
                totalMinor = totalMinor,
                statusChangedAt = createdAt,
                createdAt = createdAt,
                updatedAt = createdAt,
                payment = OrderPaymentSnapshot(
                    methodCode = PaymentMethodCode.CARD_ONLINE,
                    methodName = PaymentMethodCode.CARD_ONLINE.displayName,
                ),
            )
        ).id
    }

    private fun insertPayment(
        orderId: UUID,
        status: PaymentStatus,
        createdAt: Instant,
    ) {
        jdbcTemplate.update(
            """
            insert into payments (
                id, order_id, payment_method_code, payment_method_name, status, amount_minor, currency, provider_code,
                external_payment_id, confirmation_url, details, created_at, updated_at, paid_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID(),
            orderId,
            PaymentMethodCode.CARD_ONLINE.name,
            PaymentMethodCode.CARD_ONLINE.displayName,
            status.name,
            1_500L,
            "RUB",
            null,
            null,
            null,
            null,
            createdAt,
            createdAt,
            if (status == PaymentStatus.SUCCEEDED) createdAt else null,
        )
    }
}
