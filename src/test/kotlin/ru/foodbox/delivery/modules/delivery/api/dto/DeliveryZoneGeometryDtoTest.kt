package ru.foodbox.delivery.modules.delivery.api.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeliveryZoneGeometryDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `normalizes polygon payload into multipolygon geometry`() {
        val request = DeliveryZoneGeometryRequest(
            type = GeoJsonGeometryType.POLYGON,
            coordinates = objectMapper.readTree(
                """
                [
                  [
                    [60.6000, 56.8300],
                    [60.7000, 56.8300],
                    [60.7000, 56.9000]
                  ]
                ]
                """.trimIndent()
            ),
        )

        val geometry = request.toMultiPolygon()
        val response = geometry.toGeometryResponse()

        assertEquals(4326, geometry.srid)
        assertEquals(1, geometry.numGeometries)
        assertEquals(GeoJsonGeometryType.MULTIPOLYGON, response.type)
        assertEquals(listOf(60.6, 56.83), response.coordinates.first().first().first())
        assertEquals(
            response.coordinates.first().first().first(),
            response.coordinates.first().first().last(),
        )
    }

    @Test
    fun `rejects out of range coordinates`() {
        val request = DeliveryZoneGeometryRequest(
            type = GeoJsonGeometryType.POLYGON,
            coordinates = objectMapper.readTree(
                """
                [
                  [
                    [190.0, 56.8300],
                    [60.7000, 56.8300],
                    [60.7000, 56.9000]
                  ]
                ]
                """.trimIndent()
            ),
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            request.toMultiPolygon()
        }

        assertEquals(
            "geometry.coordinates[0][0] longitude must be between -180 and 180",
            exception.message,
        )
    }
}
