package ru.foodbox.delivery.common.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.Duration

@Configuration
@EnableConfigurationProperties(CorsProps::class)
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val corsProps: CorsProps,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.requestMatchers("/api/v1/auth/**").permitAll()
                it.requestMatchers("/api/v1/cart/**").permitAll()
                it.requestMatchers("/api/v1/catalog/**").permitAll()
                it.requestMatchers("/api/v1/delivery/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/checkout/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/payments/methods").permitAll()
                it.requestMatchers("/api/v1/payments/**").permitAll()
                it.requestMatchers("/api/v1/virtual-try-on/**").permitAll()
                it.requestMatchers("/api/v1/orders/**").permitAll()
                it.requestMatchers("/api/v1/contact-form/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                it.requestMatchers("/ws/virtual-try-on/**").permitAll()
                it.requestMatchers("/api/v1/admin/login").permitAll()
                it.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                it.requestMatchers("/api/admin/**").hasRole("ADMIN")
                it.anyRequest().authenticated()
            }
            .exceptionHandling { configurer ->
                configurer
                    .authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        // Публичный сайт
        source.registerCorsConfiguration(
            "/api/v1/auth/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/cart/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/catalog/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/delivery/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/checkout/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/orders/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/payments/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/public/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/contact-form/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/v1/virtual-try-on/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/ws/virtual-try-on/**",
            buildCorsConfiguration(corsProps.siteAllowedOrigins + corsProps.adminAllowedOrigins)
        )

        // Админка
        source.registerCorsConfiguration(
            "/api/v1/admin/**",
            buildCorsConfiguration(corsProps.adminAllowedOrigins)
        )
        source.registerCorsConfiguration(
            "/api/admin/**",
            buildCorsConfiguration(corsProps.adminAllowedOrigins)
        )

        // Фолбэк
        source.registerCorsConfiguration(
            "/**",
            buildCorsConfiguration(
                corsProps.siteAllowedOrigins + corsProps.adminAllowedOrigins
            )
        )

        return source
    }

    private fun buildCorsConfiguration(origins: List<String>): CorsConfiguration {
        return CorsConfiguration().apply {
            allowedOrigins = origins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Location")
            allowCredentials = false
            maxAge = Duration.ofHours(1).seconds
        }
    }
}
