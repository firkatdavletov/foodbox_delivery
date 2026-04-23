package ru.foodbox.delivery.modules.contactform.application

interface ContactFormService {
    fun submit(name: String, contact: String, question: String, comment: String?)
}
