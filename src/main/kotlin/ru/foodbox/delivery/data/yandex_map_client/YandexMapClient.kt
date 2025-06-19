package ru.foodbox.delivery.data.yandex_map_client

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import ru.foodbox.delivery.data.yandex_map_client.entity.GeoObject
import ru.foodbox.delivery.data.yandex_map_client.entity.YandexGeocoderResponse
import java.time.Duration

@Component
class YandexMapClient(
    @Value("\${yandex.map.api.key}") private val apiKey: String,
    @Value("\${yandex.map.baseurl}") private val baseUrl: String,
) {
    private val client = WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(5))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            )
        )
        .build()

    fun findAddress(lat: Double, lon: Double): GeoObject? {
        val response =  client.get()
            .uri { uriBuilder ->
                uriBuilder
                    .queryParam("apikey", apiKey)
                    .queryParam("geocode", "$lat,$lon")
                    .queryParam("sco", "latlong")
                    .queryParam("format", "json")
                    .queryParam("results", 1)
                    .queryParam("kind", "house")
                    .build()
            }
            .retrieve()
            .bodyToMono(YandexGeocoderResponse::class.java)
            .block() ?: throw RuntimeException("Empty response from SMS service")
        return response.response.collection.featureMember.firstOrNull()?.geoObject
    }
}