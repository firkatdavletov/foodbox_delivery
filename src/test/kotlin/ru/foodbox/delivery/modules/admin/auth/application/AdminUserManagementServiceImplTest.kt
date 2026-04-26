package ru.foodbox.delivery.modules.admin.auth.application

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.UnauthorizedException
import ru.foodbox.delivery.common.security.HashEncoder
import ru.foodbox.delivery.modules.admin.auth.application.command.ChangeOwnAdminPasswordCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.CreateAdminUserCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.ResetAdminUserPasswordCommand
import ru.foodbox.delivery.modules.admin.auth.application.command.UpdateAdminUserCommand
import ru.foodbox.delivery.modules.admin.auth.application.service.AdminUserManagementServiceImpl
import ru.foodbox.delivery.modules.admin.auth.domain.AdminAuthSession
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminAuthSessionRepository
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminUserRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminUserManagementServiceImplTest {

    private val fixedNow = Instant.parse("2026-04-26T08:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val hashEncoder = HashEncoder()

    @Test
    fun `createUser requires superadmin and stores requested role`() {
        val superadmin = adminUser(role = AdminRole.SUPERADMIN)
        val repository = InMemoryAdminUserRepository(listOf(superadmin))
        val service = service(repository)

        assertThrows<ForbiddenException> {
            service.createUser(
                actor = actor(superadmin.id, AdminRole.MANAGER),
                command = CreateAdminUserCommand(
                    login = "operator@example.com",
                    password = "operator-password",
                    role = AdminRole.ORDER_MANAGER,
                    active = true,
                ),
            )
        }

        val created = service.createUser(
            actor = actor(superadmin.id, AdminRole.SUPERADMIN),
            command = CreateAdminUserCommand(
                login = "operator@example.com",
                password = "operator-password",
                role = AdminRole.ORDER_MANAGER,
                active = true,
            ),
        )

        assertEquals(AdminRole.ORDER_MANAGER, created.role)
        assertEquals("operator@example.com", created.login)
        assertTrue(created.active)
    }

    @Test
    fun `updateUser cannot remove the last active superadmin`() {
        val superadmin = adminUser(role = AdminRole.SUPERADMIN)
        val repository = InMemoryAdminUserRepository(listOf(superadmin))
        val service = service(repository)

        assertThrows<ForbiddenException> {
            service.updateUser(
                actor = actor(UUID.randomUUID(), AdminRole.SUPERADMIN),
                id = superadmin.id,
                command = UpdateAdminUserCommand(
                    login = superadmin.login,
                    role = AdminRole.MANAGER,
                    active = true,
                ),
            )
        }
    }

    @Test
    fun `resetPassword updates password hash and revokes target sessions`() {
        val superadmin = adminUser(role = AdminRole.SUPERADMIN)
        val manager = adminUser(role = AdminRole.MANAGER, password = "old-password")
        val repository = InMemoryAdminUserRepository(listOf(superadmin, manager))
        val sessionRepository = InMemoryAdminAuthSessionRepository()
        val service = service(repository, sessionRepository)

        service.resetPassword(
            actor = actor(superadmin.id, AdminRole.SUPERADMIN),
            id = manager.id,
            command = ResetAdminUserPasswordCommand(password = "new-password"),
        )

        val updated = repository.findById(manager.id)!!
        assertTrue(hashEncoder.matches("new-password", updated.passwordHash))
        assertFalse(hashEncoder.matches("old-password", updated.passwordHash))
        assertEquals(listOf(manager.id), sessionRepository.revokedAdminIds)
    }

    @Test
    fun `changeOwnPassword works for non-superadmin role with current password`() {
        val operator = adminUser(role = AdminRole.ORDER_MANAGER, password = "old-password")
        val repository = InMemoryAdminUserRepository(listOf(operator))
        val sessionRepository = InMemoryAdminAuthSessionRepository()
        val service = service(repository, sessionRepository)

        service.changeOwnPassword(
            actor = actor(operator.id, AdminRole.ORDER_MANAGER),
            command = ChangeOwnAdminPasswordCommand(
                currentPassword = "old-password",
                newPassword = "new-password",
            ),
        )

        val updated = repository.findById(operator.id)!!
        assertTrue(hashEncoder.matches("new-password", updated.passwordHash))
        assertEquals(listOf(operator.id), sessionRepository.revokedAdminIds)
    }

    @Test
    fun `changeOwnPassword rejects invalid current password`() {
        val operator = adminUser(role = AdminRole.ORDER_MANAGER, password = "old-password")
        val repository = InMemoryAdminUserRepository(listOf(operator))
        val service = service(repository)

        assertThrows<UnauthorizedException> {
            service.changeOwnPassword(
                actor = actor(operator.id, AdminRole.ORDER_MANAGER),
                command = ChangeOwnAdminPasswordCommand(
                    currentPassword = "wrong-password",
                    newPassword = "new-password",
                ),
            )
        }
    }

    private fun service(
        repository: InMemoryAdminUserRepository,
        sessionRepository: InMemoryAdminAuthSessionRepository = InMemoryAdminAuthSessionRepository(),
    ): AdminUserManagementServiceImpl =
        AdminUserManagementServiceImpl(
            adminUserRepository = repository,
            adminAuthSessionRepository = sessionRepository,
            hashEncoder = hashEncoder,
            clock = clock,
        )

    private fun actor(id: UUID, role: AdminRole): AdminUserManagementActor =
        AdminUserManagementActor(
            adminId = id,
            roles = setOf("ADMIN", role.name),
        )

    private fun adminUser(
        id: UUID = UUID.randomUUID(),
        login: String = "admin-$id@example.com",
        role: AdminRole,
        active: Boolean = true,
        password: String = "password",
        deletedAt: Instant? = null,
    ): AdminUser =
        AdminUser(
            id = id,
            login = login,
            normalizedLogin = login.lowercase(),
            passwordHash = hashEncoder.encode(password),
            role = role,
            active = active,
            deletedAt = deletedAt,
            createdAt = fixedNow,
            updatedAt = fixedNow,
        )
}

private class InMemoryAdminUserRepository(
    initialUsers: List<AdminUser> = emptyList(),
) : AdminUserRepository {
    private val users = linkedMapOf<UUID, AdminUser>()

    init {
        initialUsers.forEach { users[it.id] = it }
    }

    override fun save(user: AdminUser): AdminUser {
        users[user.id] = user
        return user
    }

    override fun findById(id: UUID): AdminUser? = users[id]

    override fun findByNormalizedLogin(normalizedLogin: String): AdminUser? =
        users.values.firstOrNull { it.normalizedLogin == normalizedLogin }

    override fun findAllNotDeleted(): List<AdminUser> =
        users.values
            .filter { it.deletedAt == null }
            .sortedByDescending(AdminUser::createdAt)

    override fun existsByNormalizedLogin(normalizedLogin: String): Boolean =
        users.values.any { it.normalizedLogin == normalizedLogin }

    override fun existsByNormalizedLoginExceptId(normalizedLogin: String, excludedId: UUID): Boolean =
        users.values.any { it.normalizedLogin == normalizedLogin && it.id != excludedId }

    override fun countActiveByRole(role: AdminRole): Long =
        users.values.count { it.role == role && it.active && it.deletedAt == null }.toLong()

    override fun count(): Long = users.size.toLong()
}

private class InMemoryAdminAuthSessionRepository : AdminAuthSessionRepository {
    val revokedAdminIds = mutableListOf<UUID>()

    override fun save(session: AdminAuthSession): AdminAuthSession = session

    override fun findByRefreshTokenHash(hash: String): AdminAuthSession? = null

    override fun revokeById(sessionId: UUID, revokedAt: Instant) = Unit

    override fun revokeAllByAdminId(adminId: UUID, revokedAt: Instant) {
        revokedAdminIds += adminId
    }
}
