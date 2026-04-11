package ru.foodbox.delivery.common.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.NestedExceptionUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.transaction.TransactionSystemException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.sql.SQLException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

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
                traceId = currentTraceId(request),
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
                traceId = currentTraceId(request)
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
                traceId = currentTraceId(request)
            )
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val traceId = currentTraceId(request)
        logger.warn(
            "Data integrity violation on {} {} traceId={}: {}",
            request.method,
            request.requestURI,
            traceId,
            NestedExceptionUtils.getMostSpecificCause(ex).message ?: ex.message,
        )
        val message = resolveDataIntegrityMessage(ex)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                code = ErrorCode.VALIDATION_ERROR.name,
                message = message,
                traceId = traceId
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
                traceId = currentTraceId(request)
            )
        )
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        ex: ConflictException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(
                code = ErrorCode.CONFLICT.name,
                message = ex.message ?: "Conflict",
                traceId = currentTraceId(request)
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
                traceId = currentTraceId(request)
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
                traceId = currentTraceId(request)
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnknown(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val traceId = currentTraceId(request)
        val nestedValidationError = resolveNestedValidationError(ex)
        if (nestedValidationError != null) {
            logger.warn(
                "Resolved nested validation error on {} {} traceId={}: {}",
                request.method,
                request.requestURI,
                traceId,
                nestedValidationError.message,
                ex,
            )
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiError(
                    code = ErrorCode.VALIDATION_ERROR.name,
                    message = nestedValidationError.message,
                    traceId = traceId,
                )
            )
        }

        logger.error(
            "Unhandled exception on {} {} traceId={}",
            request.method,
            request.requestURI,
            traceId,
            ex,
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError(
                code = ErrorCode.INTERNAL_ERROR.name,
                message = "Internal server error",
                traceId = traceId
            )
        )
    }

    private fun currentTraceId(request: HttpServletRequest): String? {
        return MDC.get(TRACE_ID_KEY) ?: request.getHeader(TRACE_ID_HEADER)
    }

    private fun resolveNestedValidationError(ex: Exception): ResolvedValidationError? {
        val causeChain = ex.causeChain().toList()
        causeChain.filterIsInstance<IllegalArgumentException>()
            .firstOrNull()
            ?.let { return ResolvedValidationError(it.message ?: "Invalid request") }

        causeChain.filterIsInstance<ConstraintViolationException>()
            .firstOrNull()
            ?.let { return ResolvedValidationError(it.message ?: "Constraint violation") }

        causeChain.filterIsInstance<DataIntegrityViolationException>()
            .firstOrNull()
            ?.let { return ResolvedValidationError(resolveDataIntegrityMessage(it)) }

        val sqlException = causeChain.filterIsInstance<SQLException>()
            .firstOrNull(::isIntegrityConstraintViolation)
        if (sqlException != null) {
            return ResolvedValidationError(resolveDataIntegrityMessage(sqlException))
        }

        val propertyValueError = causeChain.firstOrNull { cause ->
            cause.javaClass.name == HIBERNATE_PROPERTY_VALUE_EXCEPTION
        }
        if (propertyValueError != null) {
            return ResolvedValidationError("Required value is missing")
        }

        val transactionException = causeChain.filterIsInstance<TransactionSystemException>().firstOrNull()
        if (transactionException != null) {
            val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(transactionException)
            if (mostSpecificCause != transactionException && mostSpecificCause is SQLException && isIntegrityConstraintViolation(mostSpecificCause)) {
                return ResolvedValidationError(resolveDataIntegrityMessage(mostSpecificCause))
            }
        }

        return null
    }

    private fun resolveDataIntegrityMessage(ex: Throwable): String {
        val rootCause = NestedExceptionUtils.getMostSpecificCause(ex)
        val sqlException = ex.causeChain().filterIsInstance<SQLException>().firstOrNull()
        val sqlState = sqlException?.sqlState ?: rootCause.asSqlState()
        val lowerMessage = (rootCause.message ?: ex.message ?: "").lowercase()

        val isUniqueViolation = lowerMessage.contains("duplicate key")
            || lowerMessage.contains("unique constraint")
            || lowerMessage.contains("unique index")
            || sqlState == SQL_STATE_UNIQUE_VIOLATION
        if (isUniqueViolation) {
            return "Duplicate value violates unique constraint"
        }

        val isNotNullViolation = lowerMessage.contains("not-null")
            || lowerMessage.contains("null value in column")
            || lowerMessage.contains("not null constraint")
            || sqlState == SQL_STATE_NOT_NULL_VIOLATION
        if (isNotNullViolation) {
            return "Required value is missing"
        }

        val isForeignKeyViolation = lowerMessage.contains("foreign key")
            || sqlState == SQL_STATE_FOREIGN_KEY_VIOLATION
        if (isForeignKeyViolation) {
            return "Referenced entity does not exist"
        }

        return "Data integrity violation"
    }

    private fun isIntegrityConstraintViolation(ex: SQLException): Boolean {
        return ex.sqlState in setOf(
            SQL_STATE_UNIQUE_VIOLATION,
            SQL_STATE_NOT_NULL_VIOLATION,
            SQL_STATE_FOREIGN_KEY_VIOLATION,
            SQL_STATE_CHECK_VIOLATION,
        )
    }

    private fun Throwable.asSqlState(): String? {
        return (this as? SQLException)?.sqlState
    }

    private fun Throwable.causeChain(): Sequence<Throwable> = sequence {
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = this@causeChain
        while (current != null && seen.add(current)) {
            yield(current)
            current = current.cause
        }
    }

    private data class ResolvedValidationError(
        val message: String,
    )

    private companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_KEY = "traceId"
        const val SQL_STATE_UNIQUE_VIOLATION = "23505"
        const val SQL_STATE_NOT_NULL_VIOLATION = "23502"
        const val SQL_STATE_FOREIGN_KEY_VIOLATION = "23503"
        const val SQL_STATE_CHECK_VIOLATION = "23514"
        const val HIBERNATE_PROPERTY_VALUE_EXCEPTION = "org.hibernate.PropertyValueException"
    }
}
