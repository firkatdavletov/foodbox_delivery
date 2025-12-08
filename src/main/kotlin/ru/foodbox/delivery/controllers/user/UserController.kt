package ru.foodbox.delivery.controllers.user

import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.user.body.DeleteUserResponseBody
import ru.foodbox.delivery.controllers.user.body.GetUserResponseBody
import ru.foodbox.delivery.controllers.user.body.LogoutResponseBody
import ru.foodbox.delivery.controllers.user.body.UpdateUserRequestBody
import ru.foodbox.delivery.controllers.user.body.UpdateUserResponseBody
import ru.foodbox.delivery.services.AuthService
import ru.foodbox.delivery.services.UserService
import ru.foodbox.delivery.services.dto.UserDto
import kotlin.collections.contains

@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserService,
    private val authService: AuthService,
) {
    @GetMapping()
    fun getUser(): ResponseEntity<GetUserResponseBody> {

        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            return ResponseEntity.status(401).build()
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val user = userService.getUser(userId)
        val response = GetUserResponseBody(user)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun update(@RequestBody body: UpdateUserRequestBody): ResponseEntity<UpdateUserResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            return ResponseEntity.status(401).build()
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val savedUser = userService.updateUser(userId, body.user)

        return if (savedUser != null) {
            ResponseEntity.ok(UpdateUserResponseBody(savedUser))
        } else {
            ResponseEntity.ok(UpdateUserResponseBody("Update user error", 500))
        }
    }

    @DeleteMapping
    fun delete(): ResponseEntity<DeleteUserResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            return ResponseEntity.status(401).build()
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        val result = userService.deleteUser(userId)
        val response = DeleteUserResponseBody(result, null, null)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/logout")
    fun logout(): ResponseEntity<LogoutResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            return ResponseEntity.status(401).build()
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        val success = authService.logout(userId)
        return ResponseEntity.ok(LogoutResponseBody(success, null, null))
    }
}