package ru.foodbox.delivery.data.cloudpayments_client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
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
    private val client = WebClient.builder()
        .baseUrl(baseUrl)
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
        return client.post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("$baseUrl//cards/auth")
                    .build()
            }
            .headers { headers ->
                val auth = Base64.getEncoder().encodeToString("$publicId:$secret".toByteArray())
                headers.set("Authorization", "Basic $auth")
            }
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CryptogramResponseBody::class.java)
            .block() ?: throw RuntimeException("Empty response from cloudpayments service")
    }
}