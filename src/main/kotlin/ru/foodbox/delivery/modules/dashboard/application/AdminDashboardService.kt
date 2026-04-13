package ru.foodbox.delivery.modules.dashboard.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.cart.domain.CartStatus
import ru.foodbox.delivery.modules.cart.infrastructure.persistence.jpa.CartJpaRepository
import ru.foodbox.delivery.modules.catalog.infrastructure.persistence.jpa.CatalogProductJpaRepository
import ru.foodbox.delivery.modules.dashboard.api.dto.AdminDashboardResponse
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderJpaRepository
import ru.foodbox.delivery.modules.payments.domain.PaymentStatus
import ru.foodbox.delivery.modules.payments.infrastructure.persistence.jpa.PaymentJpaRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@Service
class AdminDashboardService(
    private val orderJpaRepository: OrderJpaRepository,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val catalogProductJpaRepository: CatalogProductJpaRepository,
    private val cartJpaRepository: CartJpaRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getDashboard(): AdminDashboardResponse {
        val now = Instant.now(clock)
        val today = LocalDate.now(clock)
        val dayStart = today.atStartOfDay(clock.zone).toInstant()
        val nextDayStart = today.plusDays(1).atStartOfDay(clock.zone).toInstant()

        return AdminDashboardResponse(
            generatedAt = now,
            timeZone = clock.zone.id,
            orders = orderJpaRepository.countByCurrentStatusStateTypeIn(ACTIVE_ORDER_STATE_TYPES),
            paidToday = paymentJpaRepository.countDistinctOrderIdsByStatusAndPaidAtBetween(
                status = PaymentStatus.SUCCEEDED,
                from = dayStart,
                to = nextDayStart,
            ),
            awaitingPayment = paymentJpaRepository.countDistinctOrderIdsByLatestStatusInAndOrderStateTypeIn(
                statuses = AWAITING_PAYMENT_STATUSES,
                stateTypes = ACTIVE_ORDER_STATE_TYPES,
            ),
            newOrders = orderJpaRepository.countByCurrentStatusStateTypeIn(NEW_ORDER_STATE_TYPES),
            problematicOrders = orderJpaRepository.countByCurrentStatusStateTypeIn(PROBLEMATIC_ORDER_STATE_TYPES),
            itemsWithoutPhotos = catalogProductJpaRepository.countActiveProductsWithoutImages(),
            abandonedBaskets = cartJpaRepository.countByStatus(CartStatus.ABANDONED),
        )
    }

    private companion object {
        val ACTIVE_ORDER_STATE_TYPES = setOf(
            OrderStateType.CREATED,
            OrderStateType.AWAITING_CONFIRMATION,
            OrderStateType.CONFIRMED,
            OrderStateType.PREPARING,
            OrderStateType.READY_FOR_PICKUP,
            OrderStateType.OUT_FOR_DELIVERY,
            OrderStateType.ON_HOLD,
        )

        val NEW_ORDER_STATE_TYPES = setOf(
            OrderStateType.CREATED,
            OrderStateType.AWAITING_CONFIRMATION,
        )

        val PROBLEMATIC_ORDER_STATE_TYPES = setOf(OrderStateType.ON_HOLD)

        val AWAITING_PAYMENT_STATUSES = setOf(
            PaymentStatus.AWAITING_PAYMENT,
            PaymentStatus.PENDING,
        )
    }
}
