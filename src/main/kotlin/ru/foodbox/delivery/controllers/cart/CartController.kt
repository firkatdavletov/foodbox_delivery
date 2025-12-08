package ru.foodbox.delivery.controllers.cart

import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import ru.foodbox.delivery.controllers.cart.body.GetCartResponseBody
import ru.foodbox.delivery.controllers.cart.body.RemoveAllResponseBody
import ru.foodbox.delivery.controllers.cart.body.UpdateCartAddressRequestBody
import ru.foodbox.delivery.controllers.cart.body.UpdateQuantityRequestBody
import ru.foodbox.delivery.services.CartService

@RestController
@RequestMapping("/cart")
class CartController(
    private val cartService: CartService
) {

    @PutMapping("/updateQuantity")
    fun updateQuantity(
        @RequestBody request: UpdateQuantityRequestBody,
    ): ResponseEntity<GetCartResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            return ResponseEntity.ok(GetCartResponseBody("Нет доступа", 401))
        }

        val deviceId = (SecurityContextHolder.getContext().authentication.principal as? String)
            ?: return ResponseEntity.ok(GetCartResponseBody("Нет токена", 401))

        val cartDto =  cartService.updateItemQuantity(deviceId, request.productId, request.quantity)
        val response = GetCartResponseBody(cartDto)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/removeAll")
    fun removeAll(): ResponseEntity<RemoveAllResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            return ResponseEntity.ok(RemoveAllResponseBody("Нет доступа", 401))
        }

        val deviceId = (SecurityContextHolder.getContext().authentication.principal as? String)
            ?: return ResponseEntity.ok(RemoveAllResponseBody("Нет токена", 401))

        val cartDto = cartService.removeAll(deviceId)
        return if (cartDto != null) {
            ResponseEntity.ok(RemoveAllResponseBody(cartDto))
        } else {
            ResponseEntity.ok(RemoveAllResponseBody("Remove cart error", 100))
        }
    }

    @GetMapping
    fun getCart(): ResponseEntity<GetCartResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            return ResponseEntity.ok(GetCartResponseBody("Нет доступа", 401))
        }

        val deviceId = (SecurityContextHolder.getContext().authentication.principal as? String)
            ?: return ResponseEntity.ok(GetCartResponseBody("Нет токена", 401))

        val cart = cartService.getCart(deviceId)
        val response = if (cart != null) {
            ResponseEntity.ok(GetCartResponseBody(cart))
        } else {
            ResponseEntity.ok(GetCartResponseBody("Корзина не найдена", 404))
        }
        return response
    }

    @PostMapping("updateAddress")
    fun updateCartAddress(@RequestBody body: UpdateCartAddressRequestBody): ResponseEntity<GetCartResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            return ResponseEntity.ok(GetCartResponseBody("Нет доступа", 401))
        }

        val deviceId = (SecurityContextHolder.getContext().authentication.principal as? String)
            ?: return ResponseEntity.ok(GetCartResponseBody("Нет токена", 401))

        val cart = cartService.updateDeliveryAddress(
            deviceId = deviceId,
            deliveryType = body.deliveryType,
            deliveryAddress = body.deliveryAddress,
            departmentId = body.departmentId,
            deliveryInfo = body.deliveryInfo,
            comment = body.comment
        )

        val response = if (cart != null) {
            ResponseEntity.ok(GetCartResponseBody(cart))
        } else {
            ResponseEntity.ok(GetCartResponseBody("Ошибка обновления адреса", 400))
        }
        return response
    }
}