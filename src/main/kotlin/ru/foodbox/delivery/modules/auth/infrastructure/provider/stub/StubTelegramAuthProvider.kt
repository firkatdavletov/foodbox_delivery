package ru.foodbox.delivery.modules.auth.infrastructure.provider.stub

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.auth.infrastructure.provider.telegram.ExternalIdentityPayload
import ru.foodbox.delivery.modules.auth.infrastructure.provider.telegram.TelegramAuthProvider

@Component
class StubTelegramAuthProvider : TelegramAuthProvider {
    override fun verify(authPayload: String): ExternalIdentityPayload {
        val normalized = authPayload.trim()
        require(normalized.isNotBlank()) { "authPayload must not be blank" }

        return ExternalIdentityPayload(
            externalId = "telegram:$normalized",
            login = "telegram_$normalized",
            displayName = "Telegram User",
        )
    }
}
