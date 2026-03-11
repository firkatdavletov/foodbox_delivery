package ru.foodbox.delivery.modules.catalogimport.application.support

import org.springframework.stereotype.Component
import java.util.Locale
import java.util.UUID

@Component
class SlugNormalizer {

    fun normalize(rawSlug: String?, fallback: String): String {
        val base = rawSlug?.trim()?.takeIf { it.isNotBlank() } ?: fallback
        return base
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { UUID.randomUUID().toString() }
    }
}
