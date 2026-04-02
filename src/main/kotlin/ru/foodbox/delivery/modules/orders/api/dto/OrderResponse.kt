package ru.foodbox.delivery.modules.orders.api.dto

import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryAddressResponse
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderStatus
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.time.Instant
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val orderNumber: String,
    val customerType: OrderCustomerType,
    val userId: UUID?,
    val guestInstallId: String?,
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?,
    val status: OrderStatus,
    val payment: OrderPaymentResponse?,
    val deliveryMethod: DeliveryMethodType,
    val delivery: OrderDeliveryResponse,
    val comment: String?,
    val items: List<OrderItemResponse>,
    val subtotalMinor: Long,
    val deliveryFeeMinor: Long,
    val totalMinor: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class OrderPaymentResponse(
    val code: PaymentMethodCode,
    val name: String,
)

data class OrderItemResponse(
    val id: UUID,
    val productId: UUID,
    val variantId: UUID?,
    val sku: String?,
    val title: String,
    val unit: ProductUnit,
    val quantity: Int,
    val priceMinor: Long,
    val modifiersTotalMinor: Long,
    val totalMinor: Long,
    val modifiers: List<OrderItemModifierResponse>,
)

data class OrderItemModifierResponse(
    val modifierGroupId: UUID,
    val modifierOptionId: UUID,
    val groupCode: String,
    val groupName: String,
    val optionCode: String,
    val optionName: String,
    val applicationScope: ModifierApplicationScope,
    val priceMinor: Long,
    val quantity: Int,
)

data class OrderDeliveryResponse(
    val method: DeliveryMethodType,
    val methodName: String,
    val priceMinor: Long,
    val currency: String,
    val zoneCode: String?,
    val zoneName: String?,
    val estimatedDays: Int?,
    val estimatesMinutes: Int?,
    val pickupPointId: UUID?,
    val pickupPointExternalId: String?,
    val pickupPointName: String?,
    val pickupPointAddress: String?,
    val address: DeliveryAddressResponse?,
)
