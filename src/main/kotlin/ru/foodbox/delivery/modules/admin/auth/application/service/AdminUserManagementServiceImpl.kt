package ru.foodbox.delivery.modules.admin.auth.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.ConflictException
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.error.UnauthorizedException
import ru.foodbox.delivery.common.security.HashEncoder
import ru.foodbox.delivery.modules.admin.auth.application.AdminUserManagementActor
import ru.foodbox.delivery.modules.admin.auth.application.command.ChangeOwnAdminPasswordCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.CreateAdminUserCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.ResetAdminUserPasswordCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.UpdateAdminUserCommand
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminAuthSessionRepository
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminUserRepository
import java.time.Clock
import java.util.Locale
import java.util.UUID

@Service
class AdminUserManagementServiceImpl(
    private val adminUserRepository: AdminUserRepository,
    private val adminAuthSessionRepository: AdminAuthSessionRepository,
    private val hashEncoder: HashEncoder,
    private val clock: Clock,
) : AdminUserManagementService {

    override fun getAvailableRoles(): List<AdminRole> = AdminRole.entries

    @Transactional(readOnly = true)
    override fun getUsers(
        actor: AdminUserManagementActor,
        search: String?,
        role: AdminRole?,
        active: Boolean?,
    ): List<AdminUser> {
        requireSuperadmin(actor)

        val normalizedSearch = search?.trim()?.lowercase(Locale.ROOT).takeIf { !it.isNullOrBlank() }
        return adminUserRepository.findAllNotDeleted()
            .asSequence()
            .filter { user -> role == null || user.role == role }
            .filter { user -> active == null || user.active == active }
            .filter { user ->
                normalizedSearch == null ||
                    user.normalizedLogin.contains(normalizedSearch) ||
                    user.login.lowercase(Locale.ROOT).contains(normalizedSearch)
            }
            .toList()
    }

    @Transactional(readOnly = true)
    override fun getUser(actor: AdminUserManagementActor, id: UUID): AdminUser {
        requireSuperadmin(actor)
        return findNotDeletedUser(id)
    }

    @Transactional
    override fun createUser(actor: AdminUserManagementActor, command: CreateAdminUserCommand): AdminUser {
        requireSuperadmin(actor)

        val login = normalizeDisplayLogin(command.login)
        val normalizedLogin = normalizeLogin(login)
        ensureLoginAvailable(normalizedLogin)
        require(command.password.isNotBlank()) { "Password must not be blank" }

        val now = clock.instant()
        return adminUserRepository.save(
            AdminUser(
                id = UUID.randomUUID(),
                login = login,
                normalizedLogin = normalizedLogin,
                passwordHash = hashEncoder.encode(command.password),
                role = command.role,
                active = command.active,
                deletedAt = null,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    @Transactional
    override fun updateUser(actor: AdminUserManagementActor, id: UUID, command: UpdateAdminUserCommand): AdminUser {
        requireSuperadmin(actor)

        val existing = findNotDeletedUser(id)
        if (existing.id == actor.adminId && existing.role != command.role) {
            throw ForbiddenException("Use another superadmin account to change your own role")
        }
        if (existing.id == actor.adminId && existing.active && !command.active) {
            throw ForbiddenException("Use another superadmin account to deactivate your own user")
        }
        ensureSuperadminWillRemain(existing, command.role, command.active, deleted = false)

        val login = normalizeDisplayLogin(command.login)
        val normalizedLogin = normalizeLogin(login)
        ensureLoginAvailable(normalizedLogin, existing.id)
        val now = clock.instant()

        val updated = existing.copy(
            login = login,
            normalizedLogin = normalizedLogin,
            role = command.role,
            active = command.active,
            updatedAt = now,
        )
        val saved = adminUserRepository.save(updated)

        if (existing.role != saved.role || existing.active != saved.active) {
            adminAuthSessionRepository.revokeAllByAdminId(saved.id, now)
        }

        return saved
    }

    @Transactional
    override fun resetPassword(
        actor: AdminUserManagementActor,
        id: UUID,
        command: ResetAdminUserPasswordCommand,
    ) {
        requireSuperadmin(actor)
        if (actor.adminId == id) {
            throw ForbiddenException("Use current password flow to change your own password")
        }

        val existing = findNotDeletedUser(id)
        require(command.password.isNotBlank()) { "Password must not be blank" }
        val now = clock.instant()
        adminUserRepository.save(
            existing.copy(
                passwordHash = hashEncoder.encode(command.password),
                updatedAt = now,
            )
        )
        adminAuthSessionRepository.revokeAllByAdminId(existing.id, now)
    }

    @Transactional
    override fun changeOwnPassword(actor: AdminUserManagementActor, command: ChangeOwnAdminPasswordCommand) {
        val existing = findNotDeletedUser(actor.adminId)
        if (!existing.canAuthenticate()) {
            throw ForbiddenException("Admin user is not active")
        }
        if (!hashEncoder.matches(command.currentPassword, existing.passwordHash)) {
            throw UnauthorizedException("Current password is invalid")
        }
        require(command.newPassword.isNotBlank()) { "New password must not be blank" }

        val now = clock.instant()
        adminUserRepository.save(
            existing.copy(
                passwordHash = hashEncoder.encode(command.newPassword),
                updatedAt = now,
            )
        )
        adminAuthSessionRepository.revokeAllByAdminId(existing.id, now)
    }

    @Transactional
    override fun deleteUser(actor: AdminUserManagementActor, id: UUID) {
        requireSuperadmin(actor)
        if (actor.adminId == id) {
            throw ForbiddenException("Use another superadmin account to delete your own user")
        }

        val existing = findNotDeletedUser(id)
        ensureSuperadminWillRemain(existing, existing.role, active = false, deleted = true)

        val now = clock.instant()
        adminUserRepository.save(
            existing.copy(
                active = false,
                deletedAt = now,
                updatedAt = now,
            )
        )
        adminAuthSessionRepository.revokeAllByAdminId(existing.id, now)
    }

    private fun requireSuperadmin(actor: AdminUserManagementActor) {
        if (!actor.hasRole(AdminRole.SUPERADMIN)) {
            throw ForbiddenException("Role '${AdminRole.SUPERADMIN.name}' is required")
        }
    }

    private fun findNotDeletedUser(id: UUID): AdminUser {
        val user = adminUserRepository.findById(id) ?: throw NotFoundException("Admin user not found: $id")
        if (user.deletedAt != null) {
            throw NotFoundException("Admin user not found: $id")
        }
        return user
    }

    private fun normalizeDisplayLogin(login: String): String {
        val normalized = login.trim()
        require(normalized.isNotBlank()) { "Login must not be blank" }
        return normalized
    }

    private fun normalizeLogin(login: String): String = login.trim().lowercase(Locale.ROOT)

    private fun ensureLoginAvailable(normalizedLogin: String, excludedId: UUID? = null) {
        val exists = if (excludedId == null) {
            adminUserRepository.existsByNormalizedLogin(normalizedLogin)
        } else {
            adminUserRepository.existsByNormalizedLoginExceptId(normalizedLogin, excludedId)
        }
        if (exists) {
            throw ConflictException("Admin user with this login already exists")
        }
    }

    private fun ensureSuperadminWillRemain(
        existing: AdminUser,
        role: AdminRole,
        active: Boolean,
        deleted: Boolean,
    ) {
        val existingCountsAsSuperadmin = existing.role == AdminRole.SUPERADMIN &&
            existing.active &&
            existing.deletedAt == null
        val willCountAsSuperadmin = role == AdminRole.SUPERADMIN && active && !deleted
        if (existingCountsAsSuperadmin && !willCountAsSuperadmin) {
            val activeSuperadmins = adminUserRepository.countActiveByRole(AdminRole.SUPERADMIN)
            if (activeSuperadmins <= 1) {
                throw ForbiddenException("At least one active superadmin must remain")
            }
        }
    }
}
