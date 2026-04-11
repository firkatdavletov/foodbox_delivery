package ru.foodbox.delivery.common.error

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @AfterEach
    fun cleanupMdc() {
        MDC.clear()
    }

    @Test
    fun `handle unknown uses trace id from mdc`() {
        MDC.put("traceId", "trace-123")
        val request = MockHttpServletRequest("POST", "/api/v1/admin/catalog/products")

        val response = handler.handleUnknown(RuntimeException("boom"), request)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        val body = assertNotNull(response.body)
        assertEquals(ErrorCode.INTERNAL_ERROR.name, body.code)
        assertEquals("Internal server error", body.message)
        assertEquals("trace-123", body.traceId)
    }

    @Test
    fun `handle unknown unwraps nested sql integrity violation`() {
        MDC.put("traceId", "trace-456")
        val request = MockHttpServletRequest("POST", "/api/v1/admin/catalog/products")
        val sqlException = SQLException(
            "null value in column \"title\" violates not-null constraint",
            "23502",
        )

        val response = handler.handleUnknown(RuntimeException("transaction failed", RuntimeException(sqlException)), request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = assertNotNull(response.body)
        assertEquals(ErrorCode.VALIDATION_ERROR.name, body.code)
        assertEquals("Required value is missing", body.message)
        assertEquals("trace-456", body.traceId)
    }

    @Test
    fun `handle data integrity violation uses specific duplicate message`() {
        MDC.put("traceId", "trace-789")
        val request = MockHttpServletRequest("POST", "/api/v1/admin/catalog/products")
        val exception = DataIntegrityViolationException(
            "duplicate",
            SQLException("duplicate key value violates unique constraint", "23505"),
        )

        val response = handler.handleDataIntegrityViolation(exception, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = assertNotNull(response.body)
        assertEquals(ErrorCode.VALIDATION_ERROR.name, body.code)
        assertEquals("Duplicate value violates unique constraint", body.message)
        assertEquals("trace-789", body.traceId)
    }
}
