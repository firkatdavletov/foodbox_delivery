package ru.foodbox.delivery.modules.dashboard.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.modules.dashboard.api.dto.AdminDashboardResponse
import ru.foodbox.delivery.modules.dashboard.application.AdminDashboardService

@RestController
@RequestMapping("/api/v1/admin/dashboard")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService,
) {

    @GetMapping
    fun getDashboard(): AdminDashboardResponse {
        return adminDashboardService.getDashboard()
    }
}
