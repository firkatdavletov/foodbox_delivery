package ru.foodbox.delivery.data.cloudpayments_client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import ru.foodbox.delivery.data.cloudpayments_client.model.BankDictionaryEntity
import ru.foodbox.delivery.data.cloudpayments_client.model.CryptogramPaymentRequestBody
import ru.foodbox.delivery.data.cloudpayments_client.model.CryptogramResponseBody
import ru.foodbox.delivery.data.cloudpayments_client.model.SbpPaymentResponseBody
import ru.foodbox.delivery.data.cloudpayments_client.model.SbpPaymentRequestBody
import java.util.Base64

@Component
class CloudPaymentsClient(
    @Value("\${cloud.payments.public.id}") private val publicId: String,
    @Value("\${cloud.payments.secret}") private val secret: String
) {
    private val baseUrl = "https://api.cloudpayments.ru/payments"
    private val banksInfoQrUrl = "https://qr.nspk.ru/proxyapp/c2bmembers.json"
    private val banksInfoSubUrl = "https://sub.nspk.ru/proxyapp/c2bmembers.json"
    private val client = WebClient.builder()
        .baseUrl(baseUrl)
        .filter(ExchangeFilterFunction.ofRequestProcessor { request ->
            println("Request: ${request.method()} ${request.url()}")
            request.headers().forEach { name, values ->
                println("$name: ${values.joinToString()}")
            }
            Mono.just(request)
        })
        .filter(ExchangeFilterFunction.ofResponseProcessor { response ->
            println("Response status: ${response.statusCode()}")
            response.bodyToMono(String::class.java).doOnNext { println("Response body: $it") }.then(Mono.just(response))
        })
        .build()

    private val qrUrlClient = WebClient.builder()
        .baseUrl(banksInfoQrUrl)
        .build()

    private val subUrlClient = WebClient.builder()
        .baseUrl(banksInfoSubUrl)
        .build()

    fun payBySbp(request: SbpPaymentRequestBody): SbpPaymentResponseBody {
        return client.post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/qr/sbp/link")
                    .build()
            }
            .headers { headers ->
                val auth = Base64.getEncoder().encodeToString("$publicId:$secret".toByteArray())
                println("Authorization header: Basic $auth")
                headers.set("Authorization", "Basic $auth")
                headers.contentType = MediaType.APPLICATION_JSON
            }
            .bodyValue(request)
            .retrieve()
            .bodyToMono(SbpPaymentResponseBody::class.java)
            .block() ?: throw RuntimeException("Empty response from cloudpayments service")
    }

    fun payByCryptogram(request: CryptogramPaymentRequestBody): CryptogramResponseBody {
        try {
            return client.post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/cards/auth")
                        .build()
                }
                .headers { headers ->
                    val auth = Base64.getEncoder().encodeToString("$publicId:$secret".toByteArray())
                    headers.set("Authorization", "Basic $auth")
                    headers.contentType = MediaType.APPLICATION_JSON
                }
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CryptogramResponseBody::class.java)
                .block() ?: throw RuntimeException("Empty response from cloudpayments service")
        } catch (e: WebClientResponseException) {
            println("Ошибка CloudPayments: ${e.statusCode} - ${e.responseBodyAsString}")
            throw RuntimeException("Ошибка CloudPayments: ${e.statusCode} - ${e.responseBodyAsString}")
        }
    }

    fun getQrBanks(): BankDictionaryEntity {
        return qrUrlClient.get()
            .retrieve()
            .bodyToMono(BankDictionaryEntity::class.java)
            .block() ?: throw RuntimeException("Empty response from cloudpayments service")
    }

        fun getSubBanks(): BankDictionaryEntity {
            return subUrlClient.get()
                .retrieve()
                .bodyToMono(BankDictionaryEntity::class.java)
                .block() ?: throw RuntimeException("Empty response from cloudpayments service")
    }
}