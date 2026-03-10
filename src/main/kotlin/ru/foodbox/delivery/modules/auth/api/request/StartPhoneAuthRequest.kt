package ru.foodbox.delivery.modules.auth.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.auth.domain.AuthMethod

data class StartPhoneAuthRequest(
    @field:NotBlank
    val phone: String,

    @field:NotNull
    val method: AuthMethod // PHONE_SMS | PHONE_CALL
) {
    init {
        require(method == AuthMethod.PHONE_SMS || method == AuthMethod.PHONE_CALL) {
            "Phone start supports only PHONE_SMS or PHONE_CALL"
        }
    }
}
