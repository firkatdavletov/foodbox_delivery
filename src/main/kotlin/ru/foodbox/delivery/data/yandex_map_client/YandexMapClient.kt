package ru.foodbox.delivery.data.yandex_map_client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.netty.channel.ChannelOption
import io.netty.resolver.DefaultAddressResolverGroup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import ru.foodbox.delivery.data.yandex_map_client.entity.FeatureMember
import ru.foodbox.delivery.data.yandex_map_client.entity.SearchResultEntity
import ru.foodbox.delivery.data.yandex_map_client.entity.YandexGeocoderResponse
import java.time.Duration

@Component
class YandexMapClient(
    @Value("\${yandex.map.api.key}") private val apiKey: String,
    @Value("\${yandex.map.baseurl}") private val baseUrl: String,
    @Value("\${yandex.suggest.key}") private val suggestKey: String,
    @Value("\${yandex.suggest.url}") private val suggestUrl: String,
) {
    private val log = LoggerFactory.getLogger(YandexMapClient::class.java)
    private val objectMapper = jacksonObjectMapper()

    private val client = WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(10))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                    .resolver(DefaultAddressResolverGroup.INSTANCE)
            )
        )
        .filter(logRequest())
        .filter(logResponse())
        .build()

    private val suggestClient = WebClient.builder()
        .baseUrl(suggestUrl)
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(10))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            )
        )
        .filter(logRequest())
        .filter(logResponse())
        .build()

    private fun logRequest(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { request ->
            log.info("Request: {} {}", request.method(), request.url())
            request.headers().forEach { name, values ->
                log.info("{}: {}", name, values.joinToString())
            }
            Mono.just(request)
        }

    private fun logResponse(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofResponseProcessor { response ->
            val status = response.statusCode()

            response.bodyToMono(DataBuffer::class.java)
                .map { dataBuffer ->
                    // Читаем тело из DataBuffer
                    val bytes = ByteArray(dataBuffer.readableByteCount())
                    dataBuffer.read(bytes)
                    DataBufferUtils.release(dataBuffer)

                    val bodyString = String(bytes, Charsets.UTF_8)
                    log.info("Response status: {}", status)
                    log.info("Response body: {}", bodyString)

                    // Возвращаем тело для последующей десериализации
                    bytes
                }
                .defaultIfEmpty(ByteArray(0))
                .flatMap { bytes ->
                    val newBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes)
                    Mono.just(
                        response.mutate()
                            .body(Flux.just(newBuffer))
                            .build()
                    )
                }
        }

    fun getAddress(query: String?, uri: String?, limit: Int): List<FeatureMember> {
        val response =  client.get()
            .uri { uriBuilder ->
                if (query != null && uri == null) {
                    uriBuilder
                        .queryParam("apikey", apiKey)
                        .queryParam("geocode", query)
                        .queryParam("format", "json")
                        .queryParam("results", limit)
                        .queryParam("kind", "house")
                        .build()
                } else if (query == null && uri != null) {
                    uriBuilder
                        .queryParam("apikey", apiKey)
                        .queryParam("format", "json")
                        .queryParam("results", limit)
                        .queryParam("kind", "house")
                        .queryParam("uri", uri)
                        .build()
                } else {
                    throw IllegalArgumentException()
                }

            }
            .retrieve()
            .bodyToMono(YandexGeocoderResponse::class.java)
            .block() ?: throw RuntimeException("Empty response from yandex map service")
        return response.response.collection.featureMember
    }

    fun searchAddress(query: String, sessionToken: String): SearchResultEntity {
        val response = suggestClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .queryParam("apikey", suggestKey)
                    .queryParam("text", query)
                    .queryParam("ll", "58.407499,53.970216")
                    .queryParam("spn", "0.1,0.1")
                    .queryParam("strict_bounds", "1")
                    .queryParam("types", "house,entrance")
                    .queryParam("sessiontoken", sessionToken)
                    .queryParam("highlight", "0")
                    .queryParam("print_address", "1")
                    .queryParam("attrs", "uri")
                    .build()
            }
            .retrieve()
            .bodyToMono(SearchResultEntity::class.java)
            .block() ?: throw RuntimeException("Empty response from yandex suggest service")

        return response
    }
}