package ru.foodbox.delivery.data.telegram.model

import org.telegram.telegrambots.meta.api.objects.Update

data class RawUpdateEvent(val update: Update)