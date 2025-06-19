package ru.foodbox.delivery.controllers.map

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.controllers.map.body.GetAddressResponseBody
import ru.foodbox.delivery.services.MapService
import ru.foodbox.delivery.services.dto.GeoAddressDto

@RestController
@RequestMapping("/map")
class MapController(
    private val mapService: MapService,
) {
    @GetMapping("/address")
    fun getAddress(@RequestParam lat: Double, @RequestParam lon: Double): ResponseEntity<GetAddressResponseBody> {
        val address = mapService.findAddress(lat, lon)
        val response = GetAddressResponseBody(address)
        return ResponseEntity.ok(response)
    }
}