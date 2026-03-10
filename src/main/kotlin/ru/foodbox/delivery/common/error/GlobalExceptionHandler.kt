package ru.foodbox.delivery.common.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val details = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                code = ErrorCode.VALIDATION_ERROR.name,
                message = "Request validation failed",
                traceId = request.getHeader("X-Trace-Id"),
                details = details
            )
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                code = ErrorCode.VALIDATION_ERROR.name,
                message = ex.message ?: "Constraint violation",
                traceId = request.getHeader("X-Trace-Id")
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                code = ErrorCode.VALIDATION_ERROR.name,
                message = ex.message ?: "Invalid request",
                traceId = request.getHeader("X-Trace-Id")
            )
        )
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(
        ex: NotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(
                code = ErrorCode.NOT_FOUND.name,
                message = ex.message ?: "Resource not found",
                traceId = request.getHeader("X-Trace-Id")
            )
        )
    }

    @ExceptionHandler(ForbiddenException::class, AccessDeniedException::class)
    fun handleForbidden(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiError(
                code = ErrorCode.FORBIDDEN.name,
                message = ex.message ?: "Access denied",
                traceId = request.getHeader("X-Trace-Id")
            )
        )
    }

    @ExceptionHandler(UnauthorizedException::class, AuthenticationException::class)
    fun handleUnauthorized(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiError(
                code = ErrorCode.UNAUTHORIZED.name,
                message = ex.message ?: "Unauthorized",
                traceId = request.getHeader("X-Trace-Id")
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnknown(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError(
                code = ErrorCode.INTERNAL_ERROR.name,
                message = "Internal server error",
                traceId = request.getHeader("X-Trace-Id")
            )
        )
    }
}
