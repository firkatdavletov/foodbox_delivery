package ru.foodbox.delivery.modules.virtualtryon.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class VirtualTryOnSessionStatus(
    @get:JsonValue
    val value: String,
) {
    PENDING("pending"),
    COMPLETED("completed"),
    FAILED("failed");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): VirtualTryOnSessionStatus {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown virtual try-on session status: $value")
        }
    }
}
