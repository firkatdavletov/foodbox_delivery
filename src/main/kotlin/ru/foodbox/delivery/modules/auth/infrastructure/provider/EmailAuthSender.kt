package ru.foodbox.delivery.modules.auth.infrastructure.provider

interface EmailAuthSender {
    fun sendCode(email: String, code: String)
}