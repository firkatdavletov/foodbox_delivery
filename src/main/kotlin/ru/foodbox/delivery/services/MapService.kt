package ru.foodbox.delivery.services

import org.springframework.stereotype.Service
import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.yandex_map_client.YandexMapClient
import ru.foodbox.delivery.services.dto.DepartmentDto
import ru.foodbox.delivery.services.dto.GeoAddressDto
import ru.foodbox.delivery.services.mapper.GeoAddressMapper
import ru.foodbox.delivery.utils.DeliveryPriceCalculator
import ru.foodbox.delivery.utils.DistanceCalculator
import ru.foodbox.delivery.utils.ResultModel

@Service
class MapService(
    private val yandexMapClient: YandexMapClient,
    private val deliveryPriceCalculator: DeliveryPriceCalculator,
    private val distanceCalculator: DistanceCalculator,
    private val geoAddressMapper: GeoAddressMapper,
    private val departmentService: DepartmentService,
    private val cityService: CityService,
) {
    fun findAddress(query: String?, uri: String?, entrance: Int?): ResultModel<GeoAddressDto> {
        val geoObjects = yandexMapClient.getAddress(query,  uri, 1).map { it.geoObject }
        val geoObject = geoObjects.firstOrNull()
            ?: return ResultModel.Error("Не удалось определить адрес", 421)
        val components = geoObject.metaDataProperty.geocoderMetaData.address.components
        val cityName = components.firstOrNull { it.kind == "locality" }?.name
            ?: return ResultModel.Error("Не удалось определить адрес", 422)
        components.firstOrNull { it.kind == "street"}?.name
            ?: return ResultModel.Error("Не удалось определить адрес", 423)
        val (lng, lat) = geoObject.point.pos.split(" ").map { it.toDoubleOrNull() }

        if (lng == null || lat == null) {
            return ResultModel.Error("Не удалось определить адрес", 4424)
        }

        val city = cityService.findCityByName(cityName)
            ?: return ResultModel.Error("В данный город доставка не доступна", 404)
        val departments = departmentService.getDepartments()
        val closestDepartment = findClosestDepartment(lat, lng, departments)
            ?: return ResultModel.Error("Сюда не можем доставить", 404)

        val deliveryInfo = deliveryPriceCalculator.calculateDeliveryPrice(DeliveryType.DELIVERY, lat, lng, city.id, closestDepartment)
            ?: return ResultModel.Error("Сюда не можем доставить", 404)

        val result = geoAddressMapper.toDto(geoObject, city, deliveryInfo, 0, entrance)
        return if (result != null) {
            ResultModel.Success(result)
        } else {
            ResultModel.Error("Find address error")
        }
    }

    fun findAddress(query: String, sessionToken: String): List<GeoAddressDto> {
        val resultEntity = yandexMapClient.searchAddress(query, sessionToken)
        val addresses = resultEntity.results?.mapNotNull { entity ->
            val cityName = entity.address.component.firstOrNull { it.kind.contains("LOCALITY") }?.name
                ?: return@mapNotNull null

            val city = cityService.findCityByName(cityName)
                ?: return@mapNotNull null

            val street = entity.address.component.firstOrNull { it.kind.contains("STREET") }?.name
                ?: return@mapNotNull null
            val house = entity.address.component.firstOrNull { it.kind.contains("HOUSE") }?.name
                ?: return@mapNotNull null

            val entrance = entity.address.component.firstOrNull { it.kind.contains("ENTRANCE") }?.name?.toIntOrNull()

            GeoAddressDto(
                city = city,
                street = street,
                house = house,
                entrance = entrance,
                deliveryInfo = null,
                deliveryTime = 0,
                latitude = 0.0,
                longitude = 0.0,
                uri = entity.uri,
            )
        } ?: emptyList()
        return addresses
    }

    private fun findClosestDepartment(
        lat: Double,
        lon: Double,
        departments: List<DepartmentDto>
    ): DepartmentDto? {
        // Сначала фильтруем только работающие
        val workingDepartments = departments.filter { it.isWorkingNow }

        val candidates = workingDepartments.ifEmpty {
            departments // если нет работающих, берём всех
        }

        // Ищем ближайший
        return candidates.minByOrNull { department ->
            distanceCalculator.haversineDistance(lat, lon, department.latitude, department.longitude)
        }
    }
}