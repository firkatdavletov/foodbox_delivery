package ru.foodbox.delivery.modules.admin.auth.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.admin.auth.api.request.ChangeOwnAdminPasswordRequest
import ru.foodbox.delivery.modules.admin.auth.application.command.ChangeOwnAdminPasswordCommand
import ru.foodbox.delivery.modules.admin.auth.application.service.AdminUserManagementService

@RestController
@RequestMapping("/api/v1/admin/me")
class AdminCurrentUserController(
    private val adminUserManagementService: AdminUserManagementService,
) {

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changeOwnPassword(
        authentication: Authentication,
        @Valid @RequestBody request: ChangeOwnAdminPasswordRequest,
    ) {
        adminUserManagementService.changeOwnPassword(
            actor = authentication.toAdminActor(),
            command = ChangeOwnAdminPasswordCommand(
                currentPassword = request.currentPassword,
                newPassword = request.newPassword,
            ),
        )
    }
}
