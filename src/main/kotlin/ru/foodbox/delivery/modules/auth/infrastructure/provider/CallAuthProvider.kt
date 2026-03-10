package ru.foodbox.delivery.modules.auth.infrastructure.provider

interface CallAuthProvider {
    fun start(phone: String, code: String)
}