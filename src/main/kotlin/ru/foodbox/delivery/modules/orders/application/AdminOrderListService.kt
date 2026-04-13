package ru.foodbox.delivery.modules.orders.application

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Order as CriteriaOrder
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.embedded.DeliveryAddressEmbeddable
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListAddressResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListDeliveryResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListItemResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListManagerResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListMetaResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListPaymentResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListReferenceResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListResponse
import ru.foodbox.delivery.modules.orders.api.dto.AdminOrderListStatusResponse
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderDeliverySnapshotEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderStatusDefinitionEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderJpaRepository
import ru.foodbox.delivery.modules.payments.infrastructure.persistence.entity.PaymentEntity
import ru.foodbox.delivery.modules.payments.infrastructure.persistence.jpa.PaymentJpaRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class AdminOrderListService(
    private val orderJpaRepository: OrderJpaRepository,
    private val paymentJpaRepository: PaymentJpaRepository,
) {

    @Transactional(readOnly = true)
    fun getOrders(query: AdminOrderListQuery): AdminOrderListResponse {
        val pageable = PageRequest.of(query.page - 1, query.pageSize)
        val page = orderJpaRepository.findAll(buildSpecification(query), pageable)
        val latestPaymentsByOrderId = loadLatestPayments(page.content.map(OrderEntity::id))

        return AdminOrderListResponse(
            items = page.content.map { order ->
                order.toListItemResponse(latestPaymentsByOrderId[order.id])
            },
            meta = AdminOrderListMetaResponse(
                page = query.page,
                pageSize = query.pageSize,
                totalItems = page.totalElements,
                totalPages = page.totalPages,
                sortBy = query.effectiveSortBy.apiValue,
                sortDirection = query.sortDirection.apiValue,
            ),
        )
    }

    private fun buildSpecification(query: AdminOrderListQuery): Specification<OrderEntity> {
        return Specification { root, criteriaQuery, criteriaBuilder ->
            val queryResultType = criteriaQuery?.resultType
            val countQuery = queryResultType == Long::class.java || queryResultType == java.lang.Long::class.java
            if (criteriaQuery != null && !countQuery) {
                root.fetch<Any, Any>("currentStatus")
                root.fetch<Any, Any>("delivery")
                criteriaQuery.orderBy(buildSort(root, criteriaBuilder, query))
            }

            val status = root.get<OrderStatusDefinitionEntity>("currentStatus")
            val delivery = root.get<OrderDeliverySnapshotEntity>("delivery")
            val predicates = mutableListOf<Predicate>()

            predicates += status.get<OrderStateType>("stateType").`in`(query.scope.stateTypes)

            if (query.statusCodes.isNotEmpty()) {
                predicates += criteriaBuilder.upper(status.get("code")).`in`(query.statusCodes)
            }

            if (query.deliveryMethods.isNotEmpty()) {
                predicates += delivery.get<DeliveryMethodType>("method").`in`(query.deliveryMethods)
            }

            query.createdFrom?.let {
                predicates += criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), it)
            }

            query.createdTo?.let {
                predicates += criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), it)
            }

            query.normalizedSearch?.let { searchPattern ->
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("orderNumber")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("customerName")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("customerPhone")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("customerEmail")), searchPattern),
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    private fun buildSort(
        root: Root<OrderEntity>,
        criteriaBuilder: CriteriaBuilder,
        query: AdminOrderListQuery,
    ): List<CriteriaOrder> {
        val status = root.get<OrderStatusDefinitionEntity>("currentStatus")
        val delivery = root.get<OrderDeliverySnapshotEntity>("delivery")
        val orders = mutableListOf<CriteriaOrder>()

        fun add(path: Expression<*>) {
            orders += if (query.sortDirection == AdminOrderListSortDirection.ASC) {
                criteriaBuilder.asc(path)
            } else {
                criteriaBuilder.desc(path)
            }
        }

        when (query.effectiveSortBy) {
            AdminOrderListSortBy.ORDER_NUMBER -> add(root.get<String>("orderNumber"))
            AdminOrderListSortBy.CREATED_AT -> add(root.get<Instant>("createdAt"))
            AdminOrderListSortBy.CUSTOMER -> {
                add(root.get<String>("customerName"))
                add(root.get<String>("customerPhone"))
                add(root.get<String>("customerEmail"))
            }
            AdminOrderListSortBy.TOTAL_MINOR -> add(root.get<Long>("totalMinor"))
            AdminOrderListSortBy.PAYMENT -> {
                add(root.get<String>("paymentMethodName"))
                add(root.get<Any>("paymentMethodCode"))
            }
            AdminOrderListSortBy.DELIVERY -> {
                add(delivery.get<String>("methodName"))
                add(delivery.get<Any>("method"))
            }
            AdminOrderListSortBy.STATUS -> {
                add(status.get<Int>("sortOrder"))
                add(status.get<String>("name"))
            }
            AdminOrderListSortBy.SOURCE,
            AdminOrderListSortBy.MANAGER,
            -> add(root.get<Instant>("createdAt"))
        }

        if (query.effectiveSortBy != AdminOrderListSortBy.CREATED_AT) {
            orders += criteriaBuilder.desc(root.get<Instant>("createdAt"))
        }
        orders += criteriaBuilder.desc(root.get<java.util.UUID>("id"))
        return orders
    }

    private fun loadLatestPayments(orderIds: List<java.util.UUID>): Map<java.util.UUID, PaymentEntity> {
        if (orderIds.isEmpty()) {
            return emptyMap()
        }

        return paymentJpaRepository.findLatestByOrderIdIn(orderIds)
            .groupBy(PaymentEntity::orderId)
            .mapValues { (_, payments) ->
                payments.maxByOrNull(PaymentEntity::createdAt) ?: error("Latest payment is missing")
            }
    }

    private fun OrderEntity.toListItemResponse(paymentEntity: PaymentEntity?): AdminOrderListItemResponse {
        val deliverySnapshot = delivery ?: error("Delivery snapshot is missing for order $id")
        return AdminOrderListItemResponse(
            id = id,
            orderNumber = orderNumber,
            createdAt = createdAt,
            updatedAt = updatedAt,
            statusChangedAt = statusChangedAt,
            customerType = customerType,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            totalMinor = totalMinor,
            subtotalMinor = subtotalMinor,
            deliveryFeeMinor = deliveryFeeMinor,
            payment = paymentMethodCode?.let { methodCode ->
                AdminOrderListPaymentResponse(
                    code = methodCode,
                    name = paymentMethodName ?: methodCode.displayName,
                )
            },
            paymentStatus = paymentEntity?.let {
                AdminOrderListReferenceResponse(
                    code = it.status.name,
                    name = it.status.name,
                )
            },
            deliveryMethod = deliverySnapshot.method,
            delivery = AdminOrderListDeliveryResponse(
                method = deliverySnapshot.method,
                methodName = deliverySnapshot.methodName,
                pickupPointName = deliverySnapshot.pickupPointName,
                pickupPointAddress = deliverySnapshot.pickupPointAddress,
                address = deliverySnapshot.address?.toListAddressResponse(),
            ),
            currentStatus = AdminOrderListStatusResponse(
                id = currentStatus.id,
                code = currentStatus.code,
                name = currentStatus.name,
                stateType = currentStatus.stateType,
                isFinal = currentStatus.isFinal,
            ),
            source = null,
            manager = null,
            tags = emptyList(),
        )
    }

    private fun DeliveryAddressEmbeddable.toListAddressResponse(): AdminOrderListAddressResponse {
        return AdminOrderListAddressResponse(
            country = country,
            region = region,
            city = city,
            street = street,
            house = house,
            apartment = apartment,
            postalCode = postalCode,
            entrance = entrance,
            floor = floor,
            intercom = intercom,
        )
    }
}

data class AdminOrderListQuery(
    val page: Int = 1,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    val search: String? = null,
    val statusCodes: Set<String> = emptySet(),
    val deliveryMethods: Set<DeliveryMethodType> = emptySet(),
    val createdFrom: Instant? = null,
    val createdTo: Instant? = null,
    val scope: AdminOrderListScope = AdminOrderListScope.ALL,
    val sortBy: AdminOrderListSortBy = AdminOrderListSortBy.CREATED_AT,
    val sortDirection: AdminOrderListSortDirection = AdminOrderListSortDirection.DESC,
) {
    init {
        require(page >= 1) { "page must be greater than or equal to 1" }
        require(pageSize in SUPPORTED_PAGE_SIZES) { "pageSize must be one of 25, 50, 100" }
        require(createdFrom == null || createdTo == null || !createdFrom.isAfter(createdTo)) {
            "createdFrom must be less than or equal to createdTo"
        }
    }

    val normalizedSearch: String?
        get() = search?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { "%$it%" }

    val effectiveSortBy: AdminOrderListSortBy
        get() = when (sortBy) {
            AdminOrderListSortBy.SOURCE,
            AdminOrderListSortBy.MANAGER,
            -> AdminOrderListSortBy.CREATED_AT
            else -> sortBy
        }

    companion object {
        const val DEFAULT_PAGE_SIZE = 25
        val SUPPORTED_PAGE_SIZES: Set<Int> = setOf(25, 50, 100)
    }
}

enum class AdminOrderListScope(
    val apiValue: String,
    val stateTypes: Set<OrderStateType>,
) {
    ALL(
        apiValue = "all",
        stateTypes = setOf(
            OrderStateType.CREATED,
            OrderStateType.AWAITING_CONFIRMATION,
            OrderStateType.CONFIRMED,
            OrderStateType.PREPARING,
            OrderStateType.READY_FOR_PICKUP,
            OrderStateType.OUT_FOR_DELIVERY,
            OrderStateType.ON_HOLD,
        ),
    ),
    NEW(
        apiValue = "new",
        stateTypes = setOf(
            OrderStateType.CREATED,
            OrderStateType.AWAITING_CONFIRMATION,
        ),
    ),
    IN_WORK(
        apiValue = "in_work",
        stateTypes = setOf(
            OrderStateType.CONFIRMED,
            OrderStateType.PREPARING,
            OrderStateType.READY_FOR_PICKUP,
            OrderStateType.OUT_FOR_DELIVERY,
        ),
    ),
    PROBLEMATIC(
        apiValue = "problematic",
        stateTypes = setOf(OrderStateType.ON_HOLD),
    ),
    ;

    companion object {
        fun fromApiValue(value: String?): AdminOrderListScope {
            if (value.isNullOrBlank()) {
                return ALL
            }

            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported scope: $value")
        }
    }
}

enum class AdminOrderListSortBy(
    val apiValue: String,
) {
    ORDER_NUMBER("orderNumber"),
    CREATED_AT("createdAt"),
    CUSTOMER("customer"),
    TOTAL_MINOR("totalMinor"),
    PAYMENT("payment"),
    DELIVERY("delivery"),
    STATUS("status"),
    SOURCE("source"),
    MANAGER("manager"),
    ;

    companion object {
        fun fromApiValue(value: String?): AdminOrderListSortBy {
            if (value.isNullOrBlank()) {
                return CREATED_AT
            }

            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported sortBy: $value")
        }
    }
}

enum class AdminOrderListSortDirection(
    val apiValue: String,
) {
    ASC("asc"),
    DESC("desc"),
    ;

    companion object {
        fun fromApiValue(value: String?): AdminOrderListSortDirection {
            if (value.isNullOrBlank()) {
                return DESC
            }

            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported sortDirection: $value")
        }
    }
}

fun parseAdminOrderListBoundary(
    value: String?,
    inclusiveEndOfDay: Boolean,
): Instant? {
    val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null

    return runCatching { Instant.parse(normalized) }
        .recoverCatching { OffsetDateTime.parse(normalized).toInstant() }
        .recoverCatching { ZonedDateTime.parse(normalized).toInstant() }
        .recoverCatching { LocalDateTime.parse(normalized).toInstant(ZoneOffset.UTC) }
        .recoverCatching {
            val date = LocalDate.parse(normalized)
            if (inclusiveEndOfDay) {
                date.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toInstant()
            } else {
                date.atStartOfDay(ZoneOffset.UTC).toInstant()
            }
        }
        .getOrElse {
            throw IllegalArgumentException(
                "${if (inclusiveEndOfDay) "createdTo" else "createdFrom"} must be ISO date or ISO datetime"
            )
        }
}
