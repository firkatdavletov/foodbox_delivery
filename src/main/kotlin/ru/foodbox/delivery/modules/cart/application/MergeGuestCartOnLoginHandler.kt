package ru.foodbox.delivery.modules.cart.application

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.web.CurrentActor

@Service
class MergeGuestCartOnLoginHandler(
    private val cartService: CartService
) {

    @Transactional
    fun handle(userId: Long, installId: String?) {
        if (installId.isNullOrBlank()) return

        cartService.mergeGuestCartIntoUser(
            userActor = CurrentActor.User(
                userId = userId,
            ),
            installId = installId
        )
    }
}