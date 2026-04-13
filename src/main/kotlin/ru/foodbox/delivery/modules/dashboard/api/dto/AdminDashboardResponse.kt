package ru.foodbox.delivery.modules.dashboard.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdminDashboardResponse(
    val generatedAt: Instant,
    val timeZone: String,
    val orders: Long? = null,
    val paidToday: Long? = null,
    val awaitingPayment: Long? = null,
    val newOrders: Long? = null,
    val problematicOrders: Long? = null,
    val itemsWithoutPhotos: Long? = null,
    val abandonedBaskets: Long? = null,
)
