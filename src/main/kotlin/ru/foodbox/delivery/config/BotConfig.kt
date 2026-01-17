package ru.foodbox.delivery.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import ru.foodbox.delivery.data.telegram.FoodBoxBot

@Configuration
class BotConfig {
    @Bean
    fun telegramBotsApi(bot: FoodBoxBot): TelegramBotsApi =
        TelegramBotsApi(DefaultBotSession::class.java).apply {
            registerBot(bot)
        }
}