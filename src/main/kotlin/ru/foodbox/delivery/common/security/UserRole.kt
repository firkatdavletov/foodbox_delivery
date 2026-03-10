package ru.foodbox.delivery.common.security

enum class UserRole {
    CUSTOMER,       // обычный покупатель
    WHOLESALE,      // оптовый покупатель / контрагент
    MANAGER,        // менеджер
    ADMIN           // админ CMS
}