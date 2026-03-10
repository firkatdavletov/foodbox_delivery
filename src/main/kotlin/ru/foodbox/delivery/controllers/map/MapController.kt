package ru.foodbox.delivery.controllers.map

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.controllers.map.body.GetAddressResponseBody
import ru.foodbox.delivery.controllers.map.body.SearchAddressResponseBody
import ru.foodbox.delivery.services.MapService
import ru.foodbox.delivery.services.dto.GeoAddressDto
import ru.foodbox.delivery.common.utils.ResultModel

@RestController
@RequestMapping("/map")
class MapController(
    private val mapService: MapService,
) {
    @GetMapping("/query")
    fun getAddress(@RequestParam query: String): ResponseEntity<GetAddressResponseBody> {
        when (val result = mapService.findAddress(query, null, null)) {
            is ResultModel.Error -> {
                val response = GetAddressResponseBody(null, false, result.message, result.errorCode)
                return ResponseEntity.ok(response)
            }

            is ResultModel.Success<*> -> {
                val response = GetAddressResponseBody(result.data as GeoAddressDto, true, null, null)
                return ResponseEntity.ok(response)
            }
        }
    }

    @GetMapping("/uri")
    fun getAddressByUri(@RequestParam uri: String, @RequestParam entrance: String): ResponseEntity<GetAddressResponseBody> {
        val entrance = entrance.toIntOrNull()
        when (val result = mapService.findAddress(null, uri, entrance)) {
            is ResultModel.Error -> {
                val response = GetAddressResponseBody(null, false, result.message, result.errorCode)
                return ResponseEntity.ok(response)
            }

            is ResultModel.Success<*> -> {
                val response = GetAddressResponseBody(result.data as GeoAddressDto, true, null, null)
                return ResponseEntity.ok(response)
            }
        }
    }

    @GetMapping("/search")
    fun search(@RequestParam query: String, @RequestParam session: String): ResponseEntity<SearchAddressResponseBody> {
        val result = mapService.findAddress(query, session)
        val response = SearchAddressResponseBody(result, true, null, null)
        return ResponseEntity.ok(response)
    }
}