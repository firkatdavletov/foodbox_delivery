package ru.foodbox.delivery.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtGenerator: JwtGenerator,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith("/cart")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        val token = authHeader?.removePrefix("Bearer ")?.trim()

        if (!token.isNullOrBlank()) {
            if (jwtGenerator.validateAccessToken(token)) {
                val id = jwtGenerator.getIdFromToken(authHeader)
                val auth = UsernamePasswordAuthenticationToken(id, null, emptyList())
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        filterChain.doFilter(request, response)
    }
}