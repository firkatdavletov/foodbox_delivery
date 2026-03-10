package ru.foodbox.delivery.modules.auth.infrastructure.provider.stub

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.auth.infrastructure.provider.EmailAuthSender

@Component
class StubEmailAuthSender : EmailAuthSender {
    private val logger = LoggerFactory.getLogger(StubEmailAuthSender::class.java)

    override fun sendCode(email: String, code: String) {
        logger.info("Stub email auth code {} sent to {}", code, email)
    }
}
