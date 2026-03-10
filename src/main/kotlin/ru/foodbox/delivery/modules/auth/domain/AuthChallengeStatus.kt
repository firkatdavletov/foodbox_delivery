package ru.foodbox.delivery.modules.auth.domain

enum class AuthChallengeStatus {
    PENDING,
    VERIFIED,
    EXPIRED,
    FAILED,
    CANCELLED
}