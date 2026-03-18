package ru.foodbox.delivery.modules.virtualtryon.application

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class VirtualTryOnCategory(
    @get:JsonValue
    val value: String,
) {
    AUTO("auto"),
    TOPS("tops"),
    BOTTOMS("bottoms"),
    ONE_PIECES("one-pieces");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): VirtualTryOnCategory {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown virtual try-on category: $value")
        }
    }
}

enum class VirtualTryOnGarmentPhotoType(
    @get:JsonValue
    val value: String,
) {
    AUTO("auto"),
    FLAT_LAY("flat-lay"),
    MODEL("model");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): VirtualTryOnGarmentPhotoType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown virtual try-on garment photo type: $value")
        }
    }
}

enum class VirtualTryOnMode(
    @get:JsonValue
    val value: String,
) {
    PERFORMANCE("performance"),
    BALANCED("balanced"),
    QUALITY("quality");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): VirtualTryOnMode {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown virtual try-on mode: $value")
        }
    }
}

enum class VirtualTryOnModerationLevel(
    @get:JsonValue
    val value: String,
) {
    CONSERVATIVE("conservative"),
    PERMISSIVE("permissive"),
    NONE("none");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): VirtualTryOnModerationLevel {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown virtual try-on moderation level: $value")
        }
    }
}

enum class VirtualTryOnOutputFormat(
    @get:JsonValue
    val value: String,
) {
    PNG("png"),
    JPEG("jpeg");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): VirtualTryOnOutputFormat {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown virtual try-on output format: $value")
        }
    }
}
