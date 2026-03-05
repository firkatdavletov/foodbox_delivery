package ru.foodbox.delivery.controllers.admin.s3.body

data class InitUploadReq(
    val contentType: String,
    val sizeBytes: Long,
    val fileName: String?
)