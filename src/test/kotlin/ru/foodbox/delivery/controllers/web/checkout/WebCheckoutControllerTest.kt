package ru.foodbox.delivery.controllers.web.checkout

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.GlobalValidationHandler
import ru.foodbox.delivery.controllers.order.OrderController
import ru.foodbox.delivery.data.entities.OrderStatus
import ru.foodbox.delivery.security.JwtAuthFilter
import ru.foodbox.delivery.security.JwtCartFilter
import ru.foodbox.delivery.security.JwtGenerator
import ru.foodbox.delivery.security.SecurityConfig
import ru.foodbox.delivery.services.CartService
import ru.foodbox.delivery.services.OrderService
import ru.foodbox.delivery.services.dto.GuestCheckoutCustomerInputDto
import ru.foodbox.delivery.services.dto.GuestCheckoutDeliveryInputDto
import ru.foodbox.delivery.services.dto.GuestCheckoutItemInputDto
import ru.foodbox.delivery.services.dto.GuestCheckoutResultDto
import java.time.LocalDateTime

@WebMvcTest(controllers = [WebCheckoutController::class, OrderController::class])
@Import(SecurityConfig::class, GlobalValidationHandler::class, JwtAuthFilter::class, JwtCartFilter::class)
class WebCheckoutControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var orderService: OrderService

    @MockBean
    private lateinit var cartService: CartService

    @MockBean
    private lateinit var jwtGenerator: JwtGenerator

    @Test
    fun `guest checkout should be accessible without token`() {
        val now = LocalDateTime.of(2026, 3, 8, 12, 0, 0)
        val serviceResult = GuestCheckoutResultDto(
            orderId = 101L,
            orderNumber = "101",
            status = OrderStatus.PENDING,
            createdAt = now,
            totalAmount = 129900L
        )
        val expectedItems = listOf(
            GuestCheckoutItemInputDto(
                productId = 1,
                sku = null,
                quantity = 2
            )
        )
        val expectedCustomer = GuestCheckoutCustomerInputDto(
            name = "Иван",
            phone = "+79991234567",
            email = "ivan@example.com"
        )
        val expectedDelivery = GuestCheckoutDeliveryInputDto(
            type = ru.foodbox.delivery.data.DeliveryType.PICKUP,
            address = null,
            pickupPointId = 1
        )

        given(
            orderService.createGuestOrder(
                expectedItems,
                expectedCustomer,
                expectedDelivery,
                "Позвонить перед доставкой"
            )
        ).willReturn(serviceResult)

        val request = mapOf(
            "items" to listOf(
                mapOf(
                    "productId" to 1,
                    "quantity" to 2
                )
            ),
            "customer" to mapOf(
                "name" to "Иван",
                "phone" to "+79991234567",
                "email" to "ivan@example.com"
            ),
            "delivery" to mapOf(
                "type" to "PICKUP",
                "pickupPointId" to 1
            ),
            "comment" to "Позвонить перед доставкой"
        )

        mockMvc.perform(
            post("/api/web/checkout/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderId").value(101))
            .andExpect(jsonPath("$.orderNumber").value("101"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalAmount").value(129900))
    }

    @Test
    fun `guest checkout should return validation error when items list is empty`() {
        val request = mapOf(
            "items" to emptyList<Map<String, Any>>(),
            "customer" to mapOf(
                "name" to "Иван",
                "phone" to "+79991234567"
            ),
            "delivery" to mapOf(
                "type" to "PICKUP",
                "pickupPointId" to 1
            )
        )

        mockMvc.perform(
            post("/api/web/checkout/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("Список товаров не должен быть пустым"))

        verifyNoInteractions(orderService)
    }

    @Test
    fun `guest checkout should return validation error when phone is invalid`() {
        val request = mapOf(
            "items" to listOf(
                mapOf(
                    "productId" to 1,
                    "quantity" to 1
                )
            ),
            "customer" to mapOf(
                "name" to "Иван",
                "phone" to "abc"
            ),
            "delivery" to mapOf(
                "type" to "PICKUP",
                "pickupPointId" to 1
            )
        )

        mockMvc.perform(
            post("/api/web/checkout/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("Некорректный формат телефона"))

        verifyNoInteractions(orderService)
    }

    @Test
    fun `auth create order endpoint should still require authenticated user`() {
        val request = mapOf(
            "deliveryType" to "PICKUP",
            "deliveryAddress" to null,
            "comment" to null,
            "products" to emptyList<Map<String, Any>>(),
            "departmentId" to 1,
            "amount" to 0,
            "deliveryPrice" to 0
        )

        mockMvc.perform(
            post("/orders/createOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }
}
