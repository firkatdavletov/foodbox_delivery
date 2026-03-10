package ru.foodbox.delivery.modules.auth.infrastructure.provider.stub

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.auth.infrastructure.provider.CallAuthProvider

@Component
class StubCallAuthProvider : CallAuthProvider {
    private val logger = LoggerFactory.getLogger(StubCallAuthProvider::class.java)

    override fun start(phone: String, code: String) {
        logger.info("Stub call auth code {} initiated for {}", code, phone)
    }
}
