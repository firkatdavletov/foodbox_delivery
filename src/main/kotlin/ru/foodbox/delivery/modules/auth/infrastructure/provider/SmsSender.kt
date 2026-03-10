package ru.foodbox.delivery.modules.auth.infrastructure.provider

interface SmsSender {
    fun sendCode(phone: String, code: String)
}
