package ru.foodbox.delivery.modules.contactform.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.contactform.application.ContactFormService

@RestController
@RequestMapping("/api/v1/contact-form")
class ContactFormController(
    private val contactFormService: ContactFormService,
) {
    @PostMapping
    fun submit(@Valid @RequestBody request: ContactFormRequest) {
        contactFormService.submit(
            name = request.name,
            contact = request.contact,
            question = request.question,
            comment = request.comment,
        )
    }
}
