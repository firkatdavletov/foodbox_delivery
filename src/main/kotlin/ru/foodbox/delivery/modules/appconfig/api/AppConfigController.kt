package ru.foodbox.delivery.modules.appconfig.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.appconfig.api.response.AppConfigResponse

@RestController
@RequestMapping("/api/v1/app-config")
class AppConfigController {
    @GetMapping
    fun appConfig(): AppConfigResponse {
        return AppConfigResponse(
            minSupportedVersion = "0.0",
            maintenance = mapOf("test" to "test"),
            featureFlags = mapOf("test" to "test"),
        )
    }
}