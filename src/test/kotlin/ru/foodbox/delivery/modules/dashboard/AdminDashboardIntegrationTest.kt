package ru.foodbox.delivery.modules.dashboard

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
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.application.OrderStatusWorkflowDefaults
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
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
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardIntegrationTest {

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
        jdbcTemplate.execute("delete from cart_item_modifiers")
        jdbcTemplate.execute("delete from cart_items")
        jdbcTemplate.execute("delete from cart_delivery_drafts")
        jdbcTemplate.execute("delete from carts")
        jdbcTemplate.execute("delete from catalog_product_variant_images")
        jdbcTemplate.execute("delete from catalog_product_images")
        jdbcTemplate.execute("delete from catalog_product_variants")
        jdbcTemplate.execute("delete from product_popularity_stats")
        jdbcTemplate.execute("delete from catalog_products")
        jdbcTemplate.execute("delete from catalog_categories")
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
    fun `dashboard endpoint returns aggregated counters`() {
        val now = Instant.now()
        val todayMidday = LocalDate.now(ZoneOffset.UTC).atTime(12, 0).toInstant(ZoneOffset.UTC)
        val yesterdayMidday = todayMidday.minusSeconds(86_400)

        val categoryId = insertCategory(now)
        val firstProductId = insertProduct(categoryId = categoryId, slug = "dashboard-no-image", isActive = true, now = now)
        val secondProductId = insertProduct(categoryId = categoryId, slug = "dashboard-with-image", isActive = true, now = now)
        val thirdProductId = insertProduct(categoryId = categoryId, slug = "dashboard-with-variant-image", isActive = true, now = now)
        insertProduct(categoryId = categoryId, slug = "dashboard-inactive-no-image", isActive = false, now = now)

        insertProductImage(productId = secondProductId, now = now)
        val variantId = insertVariant(productId = thirdProductId, sku = "DASH-VARIANT-1", now = now)
        insertVariantImage(variantId = variantId, now = now)

        val onHoldStatus = insertOnHoldStatus(now)
        val pendingStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }
        val confirmedStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "CONFIRMED" }
        val completedStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "COMPLETED" }

        val pendingAwaitingPaymentOrderId = createOrder(
            orderNumber = "DASH-ORDER-1",
            status = pendingStatus,
            productId = firstProductId,
            now = now,
        )
        val pendingPaidTodayOrderId = createOrder(
            orderNumber = "DASH-ORDER-2",
            status = pendingStatus,
            productId = secondProductId,
            now = now,
        )
        createOrder(
            orderNumber = "DASH-ORDER-3",
            status = confirmedStatus,
            productId = thirdProductId,
            now = now,
        )
        createOrder(
            orderNumber = "DASH-ORDER-4",
            status = onHoldStatus,
            productId = firstProductId,
            now = now,
        )
        val completedOrderId = createOrder(
            orderNumber = "DASH-ORDER-5",
            status = completedStatus,
            productId = secondProductId,
            now = now,
        )

        insertPayment(
            orderId = pendingAwaitingPaymentOrderId,
            status = PaymentStatus.FAILED,
            createdAt = yesterdayMidday,
            paidAt = null,
        )
        insertPayment(
            orderId = pendingAwaitingPaymentOrderId,
            status = PaymentStatus.AWAITING_PAYMENT,
            createdAt = todayMidday.minusSeconds(1_800),
            paidAt = null,
        )
        insertPayment(
            orderId = pendingPaidTodayOrderId,
            status = PaymentStatus.SUCCEEDED,
            createdAt = todayMidday,
            paidAt = todayMidday,
        )
        insertPayment(
            orderId = completedOrderId,
            status = PaymentStatus.AWAITING_PAYMENT,
            createdAt = todayMidday.minusSeconds(3_600),
            paidAt = null,
        )
        insertCart(status = CartStatus.ABANDONED, now = now)
        insertCart(status = CartStatus.ACTIVE, now = now)

        val response = mockMvc.perform(get("/api/v1/admin/dashboard"))
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(response.response.contentAsByteArray)
        assertEquals(4L, body["orders"].asLong())
        assertEquals(1L, body["paidToday"].asLong())
        assertEquals(1L, body["awaitingPayment"].asLong())
        assertEquals(2L, body["newOrders"].asLong())
        assertEquals(1L, body["problematicOrders"].asLong())
        assertEquals(1L, body["itemsWithoutPhotos"].asLong())
        assertEquals(1L, body["abandonedBaskets"].asLong())
        assertTrue(body["generatedAt"].asText().isNotBlank())
        assertTrue(body["timeZone"].asText().isNotBlank())
    }

    private fun insertCategory(now: Instant): UUID {
        val categoryId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            insert into catalog_categories (
                id, external_id, name, slug, parent_id, description, sort_order, is_active, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            categoryId,
            "dashboard-category",
            "Dashboard",
            "dashboard-category",
            null,
            null,
            0,
            true,
            now,
            now,
        )
        return categoryId
    }

    private fun insertProduct(categoryId: UUID, slug: String, isActive: Boolean, now: Instant): UUID {
        val productId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            insert into catalog_products (
                id, external_id, category_id, title, slug, description, price_minor, old_price_minor, sku,
                brand, sort_order, unit, count_step, is_active, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            productId,
            slug,
            categoryId,
            slug.replace('-', ' '),
            slug,
            null,
            1_000L,
            null,
            slug.uppercase(),
            null,
            0,
            ProductUnit.PIECE.name,
            1,
            isActive,
            now,
            now,
        )
        return productId
    }

    private fun insertProductImage(productId: UUID, now: Instant) {
        jdbcTemplate.update(
            """
            insert into catalog_product_images (id, product_id, image_id, sort_order, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID(),
            productId,
            UUID.randomUUID(),
            0,
            now,
            now,
        )
    }

    private fun insertVariant(productId: UUID, sku: String, now: Instant): UUID {
        val variantId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            insert into catalog_product_variants (
                id, product_id, external_id, sku, title, price_minor, old_price_minor, sort_order, is_active, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            variantId,
            productId,
            null,
            sku,
            "Variant",
            1_100L,
            null,
            0,
            true,
            now,
            now,
        )
        return variantId
    }

    private fun insertVariantImage(variantId: UUID, now: Instant) {
        jdbcTemplate.update(
            """
            insert into catalog_product_variant_images (id, variant_id, image_id, sort_order, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID(),
            variantId,
            UUID.randomUUID(),
            0,
            now,
            now,
        )
    }

    private fun insertOnHoldStatus(now: Instant): OrderStatusDefinition {
        val statusId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            insert into order_status_definitions (
                id, code, name, description, state_type, color, icon, is_initial, is_final, is_cancellable,
                is_active, visible_to_customer, notify_customer, notify_staff, sort_order, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            statusId,
            "ON_HOLD_DASHBOARD",
            "On hold",
            "Requires attention",
            OrderStateType.ON_HOLD.name,
            "#DC2626",
            "alert-circle",
            false,
            false,
            false,
            true,
            true,
            true,
            true,
            50,
            now,
            now,
        )
        return OrderStatusDefinition(
            id = statusId,
            code = "ON_HOLD_DASHBOARD",
            name = "On hold",
            description = "Requires attention",
            stateType = OrderStateType.ON_HOLD,
            color = "#DC2626",
            icon = "alert-circle",
            isInitial = false,
            isFinal = false,
            isCancellable = false,
            isActive = true,
            visibleToCustomer = true,
            notifyCustomer = true,
            notifyStaff = true,
            sortOrder = 50,
        )
    }

    private fun createOrder(orderNumber: String, status: OrderStatusDefinition, productId: UUID, now: Instant): UUID {
        val order = orderRepository.save(
            Order(
                id = UUID.randomUUID(),
                orderNumber = orderNumber,
                customerType = OrderCustomerType.GUEST,
                userId = null,
                guestInstallId = "dashboard-install",
                customerName = "Dashboard Guest",
                customerPhone = "+79990000000",
                customerEmail = null,
                currentStatus = status,
                delivery = OrderDeliverySnapshot(
                    method = DeliveryMethodType.COURIER,
                    methodName = DeliveryMethodType.COURIER.displayName,
                    priceMinor = 500L,
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
                        productId = productId,
                        variantId = null,
                        sku = "DASH-SKU-${orderNumber.takeLast(1)}",
                        title = "Dashboard product",
                        unit = ProductUnit.PIECE,
                        quantity = 1,
                        priceMinor = 1_000L,
                        totalMinor = 1_000L,
                    )
                ),
                subtotalMinor = 1_000L,
                deliveryFeeMinor = 500L,
                totalMinor = 1_500L,
                statusChangedAt = now,
                createdAt = now,
                updatedAt = now,
            )
        )
        return order.id
    }

    private fun insertPayment(
        orderId: UUID,
        status: PaymentStatus,
        createdAt: Instant,
        paidAt: Instant?,
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
            paidAt,
        )
    }

    private fun insertCart(status: CartStatus, now: Instant) {
        jdbcTemplate.update(
            """
            insert into carts (id, owner_type, owner_id, status, total_price_minor, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID(),
            "INSTALLATION",
            UUID.randomUUID().toString(),
            status.name,
            0L,
            now,
            now,
        )
    }
}
