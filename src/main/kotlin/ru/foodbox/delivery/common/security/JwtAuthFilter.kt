package ru.foodbox.delivery.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.foodbox.delivery.modules.auth.infrastructure.jwt.JwtAccessTokenService

@Component
class JwtAuthFilter(
    private val jwtTokenService: JwtAccessTokenService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.method == HttpMethod.OPTIONS.name()) {
            filterChain.doFilter(request, response)
            return
        }

        val header = request.getHeader("Authorization")
        val token = extractBearerToken(header)

        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            val principal = jwtTokenService.parseAndValidate(token)
            if (principal != null) {
                val authorities = principal.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                val auth = UsernamePasswordAuthenticationToken(principal, null, authorities)
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(header: String?): String? {
        if (header.isNullOrBlank()) return null
        if (!header.startsWith("Bearer ")) return null
        return header.removePrefix("Bearer ").trim()
    }
}
