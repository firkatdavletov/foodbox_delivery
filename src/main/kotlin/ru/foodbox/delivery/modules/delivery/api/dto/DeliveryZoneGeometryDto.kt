package ru.foodbox.delivery.modules.delivery.api.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotNull
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.operation.valid.IsValidOp

data class DeliveryZoneGeometryRequest(
    @field:NotNull
    val type: GeoJsonGeometryType,

    @field:NotNull
    val coordinates: JsonNode,
)

data class DeliveryZoneGeometryResponse(
    val type: GeoJsonGeometryType,
    val coordinates: List<List<List<List<Double>>>>,
)

enum class GeoJsonGeometryType(
    private val wireValue: String,
) {
    POLYGON("Polygon"),
    MULTIPOLYGON("MultiPolygon"),
    ;

    @JsonValue
    fun toJson(): String = wireValue

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromJson(value: String): GeoJsonGeometryType {
            return entries.firstOrNull { type ->
                type.wireValue.equals(value, ignoreCase = true) || type.name.equals(value, ignoreCase = true)
            } ?: throw IllegalArgumentException("Unsupported geometry type: $value")
        }
    }
}

fun DeliveryZoneGeometryRequest.toMultiPolygon(): MultiPolygon {
    return DeliveryZoneGeometryJsonMapper.fromRequest(this)
}

fun MultiPolygon.toGeometryResponse(): DeliveryZoneGeometryResponse {
    return DeliveryZoneGeometryJsonMapper.toResponse(this)
}

private object DeliveryZoneGeometryJsonMapper {
    private const val SRID_WGS84 = 4326
    private val geometryFactory = GeometryFactory(PrecisionModel(), SRID_WGS84)

    fun fromRequest(request: DeliveryZoneGeometryRequest): MultiPolygon {
        val multiPolygon = when (request.type) {
            GeoJsonGeometryType.POLYGON -> geometryFactory.createMultiPolygon(
                arrayOf(parsePolygon(request.coordinates, "geometry.coordinates"))
            )

            GeoJsonGeometryType.MULTIPOLYGON -> parseMultiPolygon(request.coordinates)
        }
        multiPolygon.srid = SRID_WGS84

        validateGeometry(multiPolygon, "geometry")
        return multiPolygon
    }

    fun toResponse(geometry: MultiPolygon): DeliveryZoneGeometryResponse {
        val normalizedGeometry = geometry.copy() as MultiPolygon
        normalizedGeometry.srid = SRID_WGS84

        return DeliveryZoneGeometryResponse(
            type = GeoJsonGeometryType.MULTIPOLYGON,
            coordinates = (0 until normalizedGeometry.numGeometries).map { polygonIndex ->
                val polygon = normalizedGeometry.getGeometryN(polygonIndex) as Polygon
                buildList {
                    add(polygon.exteriorRing.toCoordinatesList())
                    repeat(polygon.numInteriorRing) { holeIndex ->
                        add(polygon.getInteriorRingN(holeIndex).toCoordinatesList())
                    }
                }
            },
        )
    }

    private fun parseMultiPolygon(node: JsonNode): MultiPolygon {
        require(node.isArray) { "geometry.coordinates must be an array for MultiPolygon geometry" }
        require(node.size() > 0) { "geometry.coordinates must contain at least one polygon" }

        val polygons = (0 until node.size()).map { polygonIndex ->
            parsePolygon(node[polygonIndex], "geometry.coordinates[$polygonIndex]")
        }

        return geometryFactory.createMultiPolygon(polygons.toTypedArray())
    }

    private fun parsePolygon(node: JsonNode, path: String): Polygon {
        require(node.isArray) { "$path must be an array of linear rings" }
        require(node.size() > 0) { "$path must contain at least one linear ring" }

        val shell = parseRing(node[0], "$path[0]")
        val holes = (1 until node.size()).map { ringIndex ->
            parseRing(node[ringIndex], "$path[$ringIndex]")
        }

        return geometryFactory.createPolygon(shell, holes.toTypedArray()).also { polygon ->
            polygon.srid = SRID_WGS84
            validateGeometry(polygon, path)
        }
    }

    private fun parseRing(node: JsonNode, path: String): LinearRing {
        require(node.isArray) { "$path must be an array of positions" }
        require(node.size() >= 3) { "$path must contain at least three positions" }

        val coordinates = (0 until node.size()).map { positionIndex ->
            parseCoordinate(node[positionIndex], "$path[$positionIndex]")
        }.toMutableList()

        if (!coordinates.first().equals2D(coordinates.last())) {
            coordinates += Coordinate(coordinates.first())
        }

        require(coordinates.size >= 4) { "$path must contain at least four positions after closing the ring" }

        return try {
            geometryFactory.createLinearRing(coordinates.toTypedArray())
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("$path is invalid: ${ex.message}")
        }
    }

    private fun parseCoordinate(node: JsonNode, path: String): Coordinate {
        require(node.isArray) { "$path must be a [longitude, latitude] array" }
        require(node.size() >= 2) { "$path must contain longitude and latitude" }
        require(node[0].isNumber && node[1].isNumber) { "$path must contain numeric longitude and latitude" }

        val longitude = node[0].doubleValue()
        val latitude = node[1].doubleValue()

        require(longitude.isFinite()) { "$path longitude must be finite" }
        require(latitude.isFinite()) { "$path latitude must be finite" }
        require(longitude in -180.0..180.0) { "$path longitude must be between -180 and 180" }
        require(latitude in -90.0..90.0) { "$path latitude must be between -90 and 90" }

        return Coordinate(longitude, latitude)
    }

    private fun validateGeometry(geometry: Geometry, path: String) {
        require(!geometry.isEmpty) { "$path must not be empty" }

        val validationError = IsValidOp(geometry).validationError
        require(validationError == null) { "$path is invalid: ${validationError.message}" }
    }

    private fun org.locationtech.jts.geom.LineString.toCoordinatesList(): List<List<Double>> {
        return coordinates.map { coordinate -> listOf(coordinate.x, coordinate.y) }
    }
}
