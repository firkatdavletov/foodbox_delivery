package ru.foodbox.delivery.modules.orders.api.dto

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import java.time.Instant
import java.util.UUID

data class AdminOrderListResponse(
    val items: List<AdminOrderListItemResponse>,
    val meta: AdminOrderListMetaResponse,
)

data class AdminOrderListItemResponse(
    val id: UUID,
    val orderNumber: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val statusChangedAt: Instant,
    val customerType: OrderCustomerType,
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?,
    val totalMinor: Long,
    val subtotalMinor: Long,
    val deliveryFeeMinor: Long,
    val payment: AdminOrderListPaymentResponse?,
    val paymentStatus: AdminOrderListReferenceResponse?,
    val deliveryMethod: DeliveryMethodType,
    val delivery: AdminOrderListDeliveryResponse,
    val currentStatus: AdminOrderListStatusResponse,
    val source: AdminOrderListReferenceResponse?,
    val manager: AdminOrderListManagerResponse?,
    val tags: List<AdminOrderListReferenceResponse>,
)

data class AdminOrderListPaymentResponse(
    val code: PaymentMethodCode,
    val name: String,
)

data class AdminOrderListReferenceResponse(
    val code: String,
    val name: String?,
)

data class AdminOrderListStatusResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val stateType: OrderStateType,
    val isFinal: Boolean,
)

data class AdminOrderListDeliveryResponse(
    val method: DeliveryMethodType,
    val methodName: String,
    val pickupPointName: String?,
    val pickupPointAddress: String?,
    val address: AdminOrderListAddressResponse?,
)

data class AdminOrderListAddressResponse(
    val country: String?,
    val region: String?,
    val city: String?,
    val street: String?,
    val house: String?,
    val apartment: String?,
    val postalCode: String?,
    val entrance: String?,
    val floor: String?,
    val intercom: String?,
)

data class AdminOrderListManagerResponse(
    val userId: UUID?,
    val displayName: String?,
)

data class AdminOrderListMetaResponse(
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
    val sortBy: String,
    val sortDirection: String,
)
