package ru.foodbox.delivery.controllers.privacy

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PrivacyPolicyController {

    @GetMapping("/privacy-policy")
    fun getPrivacyPolicyPage(): String {
        return "privacy-policy"
    }
}
