package ru.foodbox.delivery.modules.admin.auth.application.service

import ru.foodbox.delivery.modules.admin.auth.application.AdminUserManagementActor
import ru.foodbox.delivery.modules.admin.auth.application.command.ChangeOwnAdminPasswordCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.CreateAdminUserCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.ResetAdminUserPasswordCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.UpdateAdminUserCommand
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import java.util.UUID

interface AdminUserManagementService {
    fun getAvailableRoles(): List<AdminRole>
    fun getUsers(actor: AdminUserManagementActor, search: String?, role: AdminRole?, active: Boolean?): List<AdminUser>
    fun getUser(actor: AdminUserManagementActor, id: UUID): AdminUser
    fun createUser(actor: AdminUserManagementActor, command: CreateAdminUserCommand): AdminUser
    fun updateUser(actor: AdminUserManagementActor, id: UUID, command: UpdateAdminUserCommand): AdminUser
    fun resetPassword(actor: AdminUserManagementActor, id: UUID, command: ResetAdminUserPasswordCommand)
    fun changeOwnPassword(actor: AdminUserManagementActor, command: ChangeOwnAdminPasswordCommand)
    fun deleteUser(actor: AdminUserManagementActor, id: UUID)
}
