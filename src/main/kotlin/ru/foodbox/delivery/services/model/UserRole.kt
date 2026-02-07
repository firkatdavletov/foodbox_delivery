package ru.foodbox.delivery.services.model

enum class UserRole {
    CUSTOMER,       // обычный покупатель
    WHOLESALE,      // оптовый покупатель / контрагент
    MANAGER,        // менеджер
    ADMIN           // админ CMS
}