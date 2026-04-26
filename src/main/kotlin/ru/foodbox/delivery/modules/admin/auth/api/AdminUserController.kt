package ru.foodbox.delivery.modules.admin.auth.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.error.UnauthorizedException
import ru.foodbox.delivery.common.security.UserPrincipal
import ru.foodbox.delivery.modules.admin.auth.api.request.CreateAdminUserRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.ResetAdminUserPasswordRequest
import ru.foodbox.delivery.modules.admin.auth.api.request.UpdateAdminUserRequest
import ru.foodbox.delivery.modules.admin.auth.api.response.AdminRoleResponse
import ru.foodbox.delivery.modules.admin.auth.api.response.AdminUserResponse
import ru.foodbox.delivery.modules.admin.auth.application.AdminUserManagementActor
import ru.foodbox.delivery.modules.admin.auth.application.command.CreateAdminUserCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.ResetAdminUserPasswordCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.UpdateAdminUserCommand
import ru.foodbox.delivery.modules.admin.auth.application.service.AdminUserManagementService
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(
    private val adminUserManagementService: AdminUserManagementService,
) {

    @GetMapping("/roles")
    fun getRoles(): List<AdminRoleResponse> =
        adminUserManagementService.getAvailableRoles().map(AdminRole::toResponse)

    @GetMapping
    fun getUsers(
        authentication: Authentication,
        @RequestParam(name = "search", required = false) search: String?,
        @RequestParam(name = "role", required = false) role: AdminRole?,
        @RequestParam(name = "active", required = false) active: Boolean?,
    ): List<AdminUserResponse> {
        return adminUserManagementService.getUsers(authentication.toAdminActor(), search, role, active)
            .map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getUser(
        authentication: Authentication,
        @PathVariable id: UUID,
    ): AdminUserResponse =
        adminUserManagementService.getUser(authentication.toAdminActor(), id).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        authentication: Authentication,
        @Valid @RequestBody request: CreateAdminUserRequest,
    ): AdminUserResponse =
        adminUserManagementService.createUser(
            actor = authentication.toAdminActor(),
            command = CreateAdminUserCommand(
                login = request.login,
                password = request.password,
                role = request.role,
                active = request.active,
            ),
        ).toResponse()

    @PutMapping("/{id}")
    fun updateUser(
        authentication: Authentication,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAdminUserRequest,
    ): AdminUserResponse =
        adminUserManagementService.updateUser(
            actor = authentication.toAdminActor(),
            id = id,
            command = UpdateAdminUserCommand(
                login = request.login,
                role = request.role,
                active = request.active,
            ),
        ).toResponse()

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetPassword(
        authentication: Authentication,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ResetAdminUserPasswordRequest,
    ) {
        adminUserManagementService.resetPassword(
            actor = authentication.toAdminActor(),
            id = id,
            command = ResetAdminUserPasswordCommand(password = request.password),
        )
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(
        authentication: Authentication,
        @PathVariable id: UUID,
    ) {
        adminUserManagementService.deleteUser(authentication.toAdminActor(), id)
    }
}

fun Authentication.toAdminActor(): AdminUserManagementActor {
    val principal = principal as? UserPrincipal
        ?: throw UnauthorizedException("Admin principal is required")
    return AdminUserManagementActor(
        adminId = principal.userId,
        roles = authorities.map { it.authority.removePrefix("ROLE_") }.toSet(),
    )
}
