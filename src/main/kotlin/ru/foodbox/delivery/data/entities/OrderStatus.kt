package ru.foodbox.delivery.data.entities

enum class OrderStatus { PENDING, AWAITING_PAYMENT, AWAITING_CASH_PAYMENT, PAID, PROCESSING, FAILED, CANCELLED, DELIVERED }