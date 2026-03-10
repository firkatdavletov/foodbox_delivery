package ru.foodbox.delivery.modules.auth.domain

enum class AuthMethod {
    PHONE_SMS, PHONE_CALL, TELEGRAM, MAX, EMAIL;

    companion object {
        fun getAuthType(value: String): AuthMethod? {
            return when (value) {
                PHONE_SMS.name -> PHONE_SMS
                PHONE_CALL.name -> PHONE_CALL
                TELEGRAM.name -> TELEGRAM
                MAX.name -> MAX
                else -> null
            }
        }
    }
}