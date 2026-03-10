package ru.foodbox.delivery.modules.admin.auth.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.security.HashEncoder
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminUserRepository
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Component
class AdminBootstrapInitializer(
    private val adminUserRepository: AdminUserRepository,
    private val hashEncoder: HashEncoder,
    @Value("\${admin.bootstrap.login:admin}")
    private val bootstrapLogin: String,
    @Value("\${admin.bootstrap.password:password}")
    private val bootstrapPassword: String,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        if (adminUserRepository.count() > 0) {
            return
        }

        val login = bootstrapLogin.trim()
        val password = bootstrapPassword

        require(login.isNotBlank()) { "admin.bootstrap.login must not be blank" }
        require(password.isNotBlank()) { "admin.bootstrap.password must not be blank" }

        val now = Instant.now()
        adminUserRepository.save(
            AdminUser(
                id = UUID.randomUUID(),
                login = login,
                normalizedLogin = login.lowercase(Locale.ROOT),
                passwordHash = hashEncoder.encode(password),
                createdAt = now,
                updatedAt = now,
            )
        )
    }
}
