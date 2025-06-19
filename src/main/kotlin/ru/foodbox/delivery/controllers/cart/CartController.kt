package ru.foodbox.delivery.controllers.cart

import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.controllers.cart.body.*
import ru.foodbox.delivery.services.CartService

@RestController
@RequestMapping("/cart")
class CartController(
    private val cartService: CartService
) {

    @PostMapping("/updateQuantity")
    fun updateQuantity(
        @RequestBody request: UpdateQuantityRequestBody,
    ): ResponseEntity<UpdateQuantityResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val cartDto =  cartService.updateItemQuantity(userId, request.productId, request.quantity)
        val response = UpdateQuantityResponseBody(cartDto)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/removeAll")
    fun removeAll(): ResponseEntity<RemoveAllResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val cartDto =  cartService.removeAll(userId)
        val response = RemoveAllResponseBody(cartDto)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getCart(): ResponseEntity<GetCartResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val cart = cartService.getCart(userId)
        val response = GetCartResponseBody(cart)

        return ResponseEntity.ok(response)
    }

    @PutMapping("/address")
    fun updateCartAddress(
        @RequestBody request: UpdateCartAddressRequestBody
    ): ResponseEntity<UpdateCartAddressResponseBody> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")
        }

        val userId = (SecurityContextHolder.getContext().authentication.principal as String).toLongOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Access denied")

        val updatedCart = cartService.updateDeliveryAddress(
            userId = userId,
            deliveryType = request.deliveryType,
            deliveryAddress = request.deliveryAddress,
            departmentId = request.departmentId,
        )

        val response = UpdateCartAddressResponseBody(updatedCart)
        return ResponseEntity.ok(response)
    }
}