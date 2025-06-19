package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.entities.AddressEntity
import ru.foodbox.delivery.data.entities.CartEntity
import ru.foodbox.delivery.data.entities.CartItemEntity
import ru.foodbox.delivery.data.repository.AddressRepository
import ru.foodbox.delivery.data.repository.CartRepository
import ru.foodbox.delivery.data.repository.DepartmentRepository
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.data.repository.UserRepository
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.CartDto
import ru.foodbox.delivery.services.mapper.AddressMapper
import ru.foodbox.delivery.services.mapper.CartItemMapper
import ru.foodbox.delivery.services.mapper.CartMapper
import ru.foodbox.delivery.utils.DeliveryPriceCalculator
import kotlin.jvm.optionals.getOrNull

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val addressRepository: AddressRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val deliveryPriceCalculator: DeliveryPriceCalculator,
    private val cartMapper: CartMapper,
    private val addressMapper: AddressMapper,
    private val cartItemMapper: CartItemMapper,
) {

    @Transactional
    fun updateItemQuantity(userId: Long, productId: Long, quantity: Int): CartDto {
        val user = userRepository.findById(userId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")

        val cart = cartRepository.findByUserId(user.id)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Корзина не найдена")

        val product = productRepository.findById(productId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Продукт не найден")

        val existingItem = cart.items.find { it.product.id == productId }

        val updatedCart = if (existingItem != null) {
            val updatedItem = existingItem.copy(
                quantity = quantity
            )
            val updatedCartItems = if (updatedItem.quantity > 0) {
                cart.items.map {
                    if (it.id == updatedItem.id) {
                        updatedItem
                    } else {
                        it
                    }
                }
            } else {
                cart.items.filter { it.id != updatedItem.id }
            }
            val totalPrice = updatedCartItems.sumOf { it.product.price * it.quantity }

            cart.copy(
                items = updatedCartItems,
                totalPrice = totalPrice
            )
        } else {
            val newItem = CartItemEntity(
                cart = cart,
                product = product,
                quantity = quantity
            )
            val updatedCartItems = cart.items + newItem
            val totalPrice = updatedCartItems.sumOf { it.product.price * it.quantity }
            cart.copy(
                items = updatedCartItems,
                totalPrice = totalPrice
            )
        }

        val savedCart = cartRepository.save(updatedCart)
        val addressDto = savedCart.deliveryAddress?.let {
            addressMapper.toDto(it)
        }
        val cartItems = cartItemMapper.toDto(savedCart.items).sortedBy { it.title }
        return cartMapper.toDto(savedCart, cartItems, addressDto)
    }

    @Transactional
    fun removeAll(userId: Long): CartDto {
        val user = userRepository.findById(userId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")

        val cart = cartRepository.findByUserId(user.id)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Корзина не найдена")

        val updatedCart = cart.copy(items = emptyList())
        val savedCart = cartRepository.save(updatedCart)
        val addressDto = savedCart.deliveryAddress?.let {
            addressMapper.toDto(it)
        }
        val cartItems = cartItemMapper.toDto(savedCart.items)
        return cartMapper.toDto(savedCart, cartItems, addressDto)
    }

    fun getCart(userId: Long): CartDto {
        val user = userRepository.findById(userId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")

        val cart = cartRepository.findByUserId(user.id)
            ?: cartRepository.save(
                CartEntity(user = user)
            )
        val addressDto = cart.deliveryAddress?.let {
            addressMapper.toDto(it)
        }
        val cartItems = cartItemMapper.toDto(cart.items).sortedBy { it.title }
        return cartMapper.toDto(cart, cartItems, addressDto)
    }

    fun updateDeliveryAddress(
        userId: Long,
        deliveryType: DeliveryType,
        deliveryAddress: AddressDto, 
        departmentId: Long
    ): CartDto {
        val user = userRepository.findById(userId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Пользователь не найден")

        val cart = cartRepository.findByUserId(user.id)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Корзина не найдена")


        val updatedDeliveryAddress = if (cart.deliveryAddress != null) {
            cart.deliveryAddress.copy(
                lat = deliveryAddress.latitude,
                long = deliveryAddress.longitude,
                city = deliveryAddress.city,
                street = deliveryAddress.street,
                house = deliveryAddress.house,
                flat = deliveryAddress.flat,
                intercome = deliveryAddress.intercome,
                comment = deliveryAddress.comment
            )
        } else {
            addressRepository.save(
                AddressEntity(
                    lat = deliveryAddress.latitude,
                    long = deliveryAddress.longitude,
                    city = deliveryAddress.city,
                    street = deliveryAddress.street,
                    house = deliveryAddress.house,
                    flat = deliveryAddress.flat,
                    intercome = deliveryAddress.intercome,
                    comment = deliveryAddress.comment,
                    user = user
                )
            )
        }

        val deliveryPrice = when (deliveryType) {
            DeliveryType.PICKUP -> 0.0
            DeliveryType.DELIVERY -> {
                deliveryPriceCalculator.calculateDeliveryPrice(
                    updatedDeliveryAddress.lat,
                    updatedDeliveryAddress.long
                )
            }
        }

        val department = departmentRepository.findById(departmentId).getOrNull()
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Магазин не найден")

        val updatedCart = cart.copy(
            deliveryAddress = updatedDeliveryAddress,
            deliveryType = deliveryType,
            deliveryPrice = deliveryPrice,
            department = department
        )
        val savedCart = cartRepository.save(updatedCart)

        val savedAddress = savedCart.deliveryAddress?.let { addressMapper.toDto(it) }

        val cartItems = cartItemMapper.toDto(cart.items).sortedBy { it.title }

        return cartMapper.toDto(savedCart, cartItems, savedAddress)
    }
}