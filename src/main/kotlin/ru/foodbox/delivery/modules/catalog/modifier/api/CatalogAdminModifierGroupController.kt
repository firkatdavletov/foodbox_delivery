package ru.foodbox.delivery.modules.catalog.modifier.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.catalog.modifier.api.dto.ModifierGroupResponse
import ru.foodbox.delivery.modules.catalog.modifier.api.dto.ModifierOptionResponse
import ru.foodbox.delivery.modules.catalog.modifier.api.dto.UpsertModifierGroupRequest
import ru.foodbox.delivery.modules.catalog.modifier.api.dto.UpsertModifierOptionRequest
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogModifierGroupService
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierGroupCommand
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierOptionCommand
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/catalog/modifier-groups")
class CatalogAdminModifierGroupController(
    private val catalogModifierGroupService: CatalogModifierGroupService,
) {

    @GetMapping
    fun getModifierGroups(
        @RequestParam(name = "isActive", required = false) isActive: Boolean?,
    ): List<ModifierGroupResponse> {
        return catalogModifierGroupService.getAll(isActive).map(ModifierGroup::toResponse)
    }

    @PostMapping
    fun upsertModifierGroup(
        @Valid @RequestBody request: UpsertModifierGroupRequest,
    ): ModifierGroupResponse {
        return catalogModifierGroupService.upsertGroup(
            UpsertModifierGroupCommand(
                id = request.id,
                code = request.code,
                name = request.name,
                minSelected = request.minSelected,
                maxSelected = request.maxSelected,
                isRequired = request.isRequired,
                isActive = request.isActive,
                sortOrder = request.sortOrder,
                options = emptyList(),
            )
        ).toResponse()
    }

    @GetMapping("/{groupId}/options")
    fun getModifierGroupOptions(
        @PathVariable groupId: UUID,
        @RequestParam(name = "isActive", required = false) isActive: Boolean?,
    ): List<ModifierOptionResponse> {
        return catalogModifierGroupService.getOptions(groupId, isActive).map(ModifierOption::toResponse)
    }

    @GetMapping("/{groupId}/options/{optionId}")
    fun getModifierGroupOption(
        @PathVariable groupId: UUID,
        @PathVariable optionId: UUID,
    ): ModifierOptionResponse {
        return catalogModifierGroupService.getOption(groupId, optionId).toResponse()
    }

    @PostMapping("/{groupId}/options")
    fun upsertModifierGroupOption(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: UpsertModifierOptionRequest,
    ): ModifierOptionResponse {
        return catalogModifierGroupService.upsertOption(
            groupId = groupId,
            optionId = request.id,
            command = request.toCommand(),
        ).toResponse()
    }
}

private fun ModifierGroup.toResponse(): ModifierGroupResponse {
    return ModifierGroupResponse(
        id = id,
        code = code,
        name = name,
        minSelected = minSelected,
        maxSelected = maxSelected,
        isRequired = isRequired,
        isActive = isActive,
        sortOrder = sortOrder,
    )
}

private fun ModifierOption.toResponse(): ModifierOptionResponse {
    return ModifierOptionResponse(
        id = id,
        code = code,
        name = name,
        description = description,
        priceType = priceType,
        price = price,
        applicationScope = applicationScope,
        isDefault = isDefault,
        isActive = isActive,
        sortOrder = sortOrder,
    )
}

private fun UpsertModifierOptionRequest.toCommand(): UpsertModifierOptionCommand {
    return UpsertModifierOptionCommand(
        code = code,
        name = name,
        description = description,
        priceType = priceType,
        price = price,
        applicationScope = applicationScope,
        isDefault = isDefault,
        isActive = isActive,
        sortOrder = sortOrder,
    )
}
