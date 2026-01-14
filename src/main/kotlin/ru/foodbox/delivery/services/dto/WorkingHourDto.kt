package ru.foodbox.delivery.services.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.DayOfWeek
import java.time.LocalTime

data class WorkingHourDto(
    val dayOfWeek: DayOfWeek,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val openTime: LocalTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val closeTime: LocalTime,
    val workTime: String,
)