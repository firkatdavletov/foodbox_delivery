package ru.foodbox.delivery.modules.admin.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.foodbox.delivery.common.security.HashEncoder
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminUserRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserManagementIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var adminUserRepository: AdminUserRepository

    @Autowired
    private lateinit var hashEncoder: HashEncoder

    private val superadminLogin = "root@example.com"
    private val superadminPassword = "root-password"

    @BeforeEach
    fun resetAdminUsers() {
        jdbcTemplate.execute("delete from admin_auth_session")
        jdbcTemplate.execute("delete from admin_user")

        val now = Instant.now()
        adminUserRepository.save(
            AdminUser(
                id = UUID.randomUUID(),
                login = superadminLogin,
                normalizedLogin = superadminLogin,
                passwordHash = hashEncoder.encode(superadminPassword),
                role = AdminRole.SUPERADMIN,
                active = true,
                deletedAt = null,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    @Test
    fun `superadmin can create and list admin users through API`() {
        val token = login(superadminLogin, superadminPassword)
        val managerLogin = "manager-${UUID.randomUUID()}@example.com"

        val createResponse = mockMvc.perform(
            post("/api/v1/admin/users")
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsBytes(
                        mapOf(
                            "login" to managerLogin,
                            "password" to "manager-password",
                            "role" to AdminRole.ORDER_MANAGER.name,
                            "active" to true,
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = objectMapper.readTree(createResponse.response.contentAsByteArray)
        assertEquals(managerLogin, created["login"].asText())
        assertEquals(AdminRole.ORDER_MANAGER.name, created["role"].asText())

        val listResponse = mockMvc.perform(
            get("/api/v1/admin/users")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andReturn()

        val users = objectMapper.readTree(listResponse.response.contentAsByteArray)
        assertTrue(users.any { it["login"].asText() == managerLogin })
    }

    @Test
    fun `non-superadmin cannot create admin users through API`() {
        val superadminToken = login(superadminLogin, superadminPassword)
        val managerLogin = "manager-${UUID.randomUUID()}@example.com"

        mockMvc.perform(
            post("/api/v1/admin/users")
                .header("Authorization", "Bearer $superadminToken")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsBytes(
                        mapOf(
                            "login" to managerLogin,
                            "password" to "manager-password",
                            "role" to AdminRole.MANAGER.name,
                            "active" to true,
                        )
                    )
                )
        )
            .andExpect(status().isCreated)

        val managerToken = login(managerLogin, "manager-password")
        mockMvc.perform(
            post("/api/v1/admin/users")
                .header("Authorization", "Bearer $managerToken")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsBytes(
                        mapOf(
                            "login" to "support-${UUID.randomUUID()}@example.com",
                            "password" to "support-password",
                            "role" to AdminRole.SUPPORT.name,
                            "active" to true,
                        )
                    )
                )
        )
            .andExpect(status().isForbidden)
    }

    private fun login(login: String, password: String): String {
        val response = mockMvc.perform(
            post("/api/v1/admin/login")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsBytes(
                        mapOf(
                            "login" to login,
                            "password" to password,
                            "deviceId" to "test-device",
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(response.response.contentAsByteArray)["accessToken"].asText()
    }
}
