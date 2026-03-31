package ru.foodbox.delivery.modules.catalog.modifier.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.catalog.modifier.api.dto.ModifierGroupResponse
import ru.foodbox.delivery.modules.catalog.modifier.api.dto.ModifierOptionResponse
import ru.foodbox.delivery.modules.catalog.modifier.api.dto.UpsertModifierGroupRequest
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogModifierGroupService
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierGroupCommand
import ru.foodbox.delivery.modules.catalog.modifier.application.command.UpsertModifierOptionCommand
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroupWithOptions

@RestController
@RequestMapping("/api/v1/admin/catalog/modifier-groups")
class CatalogAdminModifierGroupController(
    private val catalogModifierGroupService: CatalogModifierGroupService,
) {

    @GetMapping
    fun getModifierGroups(
        @RequestParam(name = "isActive", required = false) isActive: Boolean?,
    ): List<ModifierGroupResponse> {
        return catalogModifierGroupService.getAll(isActive).map(ModifierGroupWithOptions::toResponse)
    }

    @PostMapping
    fun upsertModifierGroup(
        @Valid @RequestBody request: UpsertModifierGroupRequest,
    ): ModifierGroupResponse {
        return catalogModifierGroupService.upsert(
            UpsertModifierGroupCommand(
                id = request.id,
                code = request.code,
                name = request.name,
                minSelected = request.minSelected,
                maxSelected = request.maxSelected,
                isRequired = request.isRequired,
                isActive = request.isActive,
                sortOrder = request.sortOrder,
                options = request.options.map { option ->
                    UpsertModifierOptionCommand(
                        code = option.code,
                        name = option.name,
                        description = option.description,
                        priceType = option.priceType,
                        price = option.price,
                        applicationScope = option.applicationScope,
                        isDefault = option.isDefault,
                        isActive = option.isActive,
                        sortOrder = option.sortOrder,
                    )
                },
            )
        ).toResponse()
    }
}

private fun ModifierGroupWithOptions.toResponse(): ModifierGroupResponse {
    return ModifierGroupResponse(
        id = group.id,
        code = group.code,
        name = group.name,
        minSelected = group.minSelected,
        maxSelected = group.maxSelected,
        isRequired = group.isRequired,
        isActive = group.isActive,
        sortOrder = group.sortOrder,
        options = options.map { option ->
            ModifierOptionResponse(
                id = option.id,
                code = option.code,
                name = option.name,
                description = option.description,
                priceType = option.priceType,
                price = option.price,
                applicationScope = option.applicationScope,
                isDefault = option.isDefault,
                isActive = option.isActive,
                sortOrder = option.sortOrder,
            )
        },
    )
}
