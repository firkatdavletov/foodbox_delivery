package ru.foodbox.delivery.common.web

import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import ru.foodbox.delivery.common.error.UnauthorizedException
import ru.foodbox.delivery.common.security.UserPrincipal

@Component
class CurrentActorArgumentResolver : HandlerMethodArgumentResolver {

    companion object {
        private const val DEVICE_ID_HEADER = "X-Device-Id"
        private const val INSTALL_ID_HEADER = "X-Install-Id"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == CurrentActor::class.java &&
            parameter.hasParameterAnnotation(CurrentActorParam::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal

        if (principal is UserPrincipal) {
            return CurrentActor.User(
                userId = principal.userId,
                roles = principal.roles
            )
        }

        val installId = webRequest.getHeader(DEVICE_ID_HEADER)?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: webRequest.getHeader(INSTALL_ID_HEADER)?.trim()?.takeIf { it.isNotBlank() }
        if (!installId.isNullOrBlank()) {
            return CurrentActor.Guest(installId = installId)
        }

        throw UnauthorizedException("Either bearer token or X-Device-Id header is required")
    }
}
