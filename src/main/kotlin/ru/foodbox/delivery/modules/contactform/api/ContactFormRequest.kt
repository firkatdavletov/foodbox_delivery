package ru.foodbox.delivery.modules.contactform.api

import jakarta.validation.constraints.NotBlank

data class ContactFormRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val contact: String,
    @field:NotBlank val question: String,
    val comment: String? = null,
)
