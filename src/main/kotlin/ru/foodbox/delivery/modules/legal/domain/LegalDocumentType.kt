package ru.foodbox.delivery.modules.legal.domain

enum class LegalDocumentType(
    val slug: String,
    val defaultTitle: String,
) {
    PUBLIC_OFFER(
        slug = "public-offer",
        defaultTitle = "ПУБЛИЧНАЯ ОФЕРТА о продаже товаров дистанционным способом",
    ),
    PERSONAL_DATA_CONSENT(
        slug = "personal-data-consent",
        defaultTitle = "СОГЛАСИЕ НА ОБРАБОТКУ ПЕРСОНАЛЬНЫХ ДАННЫХ",
    ),
    PERSONAL_DATA_POLICY(
        slug = "personal-data-policy",
        defaultTitle = "ПОЛИТИКА ОБРАБОТКИ ПЕРСОНАЛЬНЫХ ДАННЫХ",
    ),
    ;

    companion object {
        fun fromSlug(slug: String): LegalDocumentType {
            return entries.firstOrNull { it.slug == slug }
                ?: throw IllegalArgumentException("Unknown legal document type: $slug")
        }
    }
}
