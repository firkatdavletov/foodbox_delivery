package ru.foodbox.delivery.modules.catalog.modifier.application.command

import java.util.UUID

data class ReplaceProductModifierGroupCommand(
    val modifierGroupId: UUID,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
)
