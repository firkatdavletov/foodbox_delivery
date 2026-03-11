package ru.foodbox.delivery.modules.catalogimport.domain

enum class CatalogImportErrorCode {
    MISSING_REQUIRED_FIELD,
    INVALID_BOOLEAN,
    INVALID_NUMBER,
    DUPLICATE_KEY_IN_FILE,
    CATEGORY_NOT_FOUND,
    PARENT_CATEGORY_NOT_FOUND,
    AMBIGUOUS_MATCH,
    INVALID_RELATION,
    PERSISTENCE_ERROR,
}
