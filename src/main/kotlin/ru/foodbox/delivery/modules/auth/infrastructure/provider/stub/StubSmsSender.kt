package ru.foodbox.delivery.modules.auth.infrastructure.provider.stub

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.auth.infrastructure.provider.SmsSender

@Component
class StubSmsSender : SmsSender {
    private val logger = LoggerFactory.getLogger(StubSmsSender::class.java)

    override fun sendCode(phone: String, code: String) {
        logger.info("Stub SMS auth code {} sent to {}", code, phone)
    }
}
