package ru.foodbox.delivery.data.sms_client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class SmsClient(
    @param:Value("sms.ru.api.key") private val api: String
) {
    private val baseUrl = "https://sms.ru"

    private val client = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun sendSmsCode(phone: String, code: String): SmsRuResponseEntity {
        val uri = "/sms/send"

        return client.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(uri)
                    .queryParam("api_id", api)
                    .queryParam("to", phone)
                    .queryParam("msg", code)
                    .queryParam("json", 1)
                    .build()
            }
            .retrieve()
            .bodyToMono(SmsRuResponseEntity::class.java)
            .block() ?: throw RuntimeException("Empty response from SMS service")
    }
}