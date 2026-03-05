package ru.foodbox.delivery.controllers.admin.s3.body

import java.util.UUID

data class InitUploadRes(
    val imageId: UUID,
    val objectKey: String,
    val uploadUrl: String,
    val requiredHeaders: Map<String,String>
)