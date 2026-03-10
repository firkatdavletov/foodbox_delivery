package ru.foodbox.delivery.modules.auth.infrastructure.provider.stub

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.auth.infrastructure.provider.max.ExternalIdentityPayload
import ru.foodbox.delivery.modules.auth.infrastructure.provider.max.MaxAuthProvider

@Component
class StubMaxAuthProvider : MaxAuthProvider {
    override fun verify(authPayload: String): ExternalIdentityPayload {
        val normalized = authPayload.trim()
        require(normalized.isNotBlank()) { "authPayload must not be blank" }

        return ExternalIdentityPayload(
            externalId = "max:$normalized",
            login = "max_$normalized",
            displayName = "Max User",
        )
    }
}
