package ru.foodbox.delivery.modules.notifications.infrastructure.telegram

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "telegram")
class TelegramProperties {
    var enabled: Boolean = false
    var botToken: String = ""
    var defaultChatIds: List<String> = emptyList()
}
