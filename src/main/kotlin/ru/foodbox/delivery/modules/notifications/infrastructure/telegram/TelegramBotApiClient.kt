package ru.foodbox.delivery.modules.notifications.infrastructure.telegram

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import ru.foodbox.delivery.modules.notifications.application.TelegramMessageGateway
import ru.foodbox.delivery.modules.notifications.application.TelegramSendOutcome

@Component
class TelegramBotApiClient(
    private val telegramProperties: TelegramProperties,
    restClientBuilder: RestClient.Builder,
) : TelegramMessageGateway {

    private val logger = LoggerFactory.getLogger(TelegramBotApiClient::class.java)
    private val restClient: RestClient = run {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(CONNECT_TIMEOUT_MS)
            setReadTimeout(READ_TIMEOUT_MS)
        }

        restClientBuilder
            .baseUrl(TELEGRAM_BASE_URL)
            .requestFactory(requestFactory)
            .build()
    }

    override fun sendMessage(chatId: String, text: String): TelegramSendOutcome {
        val botToken = telegramProperties.botToken.trim()
        if (botToken.isBlank()) {
            return TelegramSendOutcome(delivered = false, reason = "Telegram bot token is blank")
        }

        return try {
            val response = restClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .body(SendMessageRequest(chatId = chatId, text = text))
                .retrieve()
                .body(TelegramApiResponse::class.java)

            if (response?.ok == true) {
                TelegramSendOutcome(delivered = true)
            } else {
                TelegramSendOutcome(
                    delivered = false,
                    reason = response?.description ?: "Telegram API returned ok=false",
                )
            }
        } catch (ex: RestClientResponseException) {
            logger.warn(
                "Telegram API HTTP error while sending to chatId={} status={} body={}",
                chatId,
                ex.statusCode.value(),
                ex.responseBodyAsString.take(MAX_ERROR_BODY_LOG_LENGTH),
            )
            TelegramSendOutcome(
                delivered = false,
                reason = "Telegram API HTTP error ${ex.statusCode.value()}",
            )
        } catch (ex: Exception) {
            logger.warn(
                "Telegram API error while sending to chatId={} errorType={}",
                chatId,
                ex.javaClass.simpleName,
            )
            TelegramSendOutcome(
                delivered = false,
                reason = "Telegram API error ${ex.javaClass.simpleName}",
            )
        }
    }

    private data class SendMessageRequest(
        @JsonProperty("chat_id")
        val chatId: String,
        val text: String,
        @JsonProperty("disable_web_page_preview")
        val disableWebPagePreview: Boolean = true,
    )

    private data class TelegramApiResponse(
        val ok: Boolean = false,
        val description: String? = null,
    )

    companion object {
        private const val TELEGRAM_BASE_URL = "https://api.telegram.org"
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 5_000
        private const val MAX_ERROR_BODY_LOG_LENGTH = 300
    }
}
