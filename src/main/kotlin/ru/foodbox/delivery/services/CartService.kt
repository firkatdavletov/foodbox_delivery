package ru.foodbox.delivery.services

import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.DeliveryType
import ru.foodbox.delivery.data.entities.AddressEntity
import ru.foodbox.delivery.data.entities.CartEntity
import ru.foodbox.delivery.data.entities.CartItemEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.data.repository.*
import ru.foodbox.delivery.security.JwtGenerator
import ru.foodbox.delivery.services.dto.AddressDto
import ru.foodbox.delivery.services.dto.CartDto
import ru.foodbox.delivery.services.mapper.CartMapper
import ru.foodbox.delivery.services.mapper.DepartmentMapper
import ru.foodbox.delivery.services.model.DeliveryInfo
import ru.foodbox.delivery.utils.DeliveryPriceCalculator
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val departmentRepository: DepartmentRepository,
    private val cityRepository: CityRepository,
    private val cartMapper: CartMapper,
    private val departmentMapper: DepartmentMapper,
    private val addressRepository: AddressRepository,
    private val jwtGenerator: JwtGenerator,
    private val deliveryPriceCalculator: DeliveryPriceCalculator,
) {

    @Transactional
    fun createCart(
        deviceId: String,
        departmentId: Int,
        deliveryType: DeliveryType,
        deliveryAddress: AddressDto?,
        deliveryPrice: BigDecimal,
        freeDeliveryPrice: BigDecimal?,
    ): String? {
        val department = departmentRepository.findById(departmentId.toLong()).getOrNull() ?: return null

        val address = deliveryAddress?.let {
            val city = cityRepository.findById(it.city.id).getOrNull() ?: return null
            val addressEntity = AddressEntity(
                street = it.street,
                house = it.house,
                entrance = it.entrance,
                flat = it.flat,
                intercome = it.intercome,
                comment = it.comment,
                cityEntity = city,
                latitude = it.latitude,
                longitude = it.longitude
            )
            addressRepository.save(addressEntity)
        }
        val newCart = CartEntity(
            deviceId = deviceId,
            department = department,
            deliveryType = deliveryType,
            deliveryAddress = address,
            deliveryPrice = deliveryPrice,
            freeDeliveryPrice = freeDeliveryPrice,
            minPriceForOrder = BigDecimal.ZERO,
            discountPrice = BigDecimal.ZERO,
            totalPrice = BigDecimal.ZERO,
            comment = null,
        )
        val savedCart = cartRepository.save(newCart)
        val token = jwtGenerator.generateCartToken(savedCart.deviceId)
        return token
    }

    @Transactional
    fun updateItemQuantity(deviceId: String, productId: Long, quantity: Int): CartDto {
        if (quantity < 0) {
            throw ResponseStatusException(HttpStatusCode.valueOf(400), "Количество не может быть отрицательным")
        }

        val cart = cartRepository.findByDeviceId(deviceId)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Cart not found")

        val existingItem = cart.items.find { it.product.id == productId }

        if (existingItem != null) {
            if (quantity == 0) {
                cart.removeItem { existingItem }
            } else {
                existingItem.quantity = quantity
            }
        } else {
            if (quantity == 0) {
                return cartMapper.toDto(cart)
            }

            val product = getProductForCart(productId)
            validateQuantityStep(quantity, product.countStep)

            val newCartItem = CartItemEntity(
                cart = cart,
                product = product,
                quantity = quantity,
            )
            newCartItem.created = LocalDateTime.now()
            newCartItem.modified = LocalDateTime.now()
            cart.addItem { newCartItem }
        }

        cart.updateTotalPrice()

        val savedCart = cartRepository.save(cart)

        return cartMapper.toDto(savedCart)
    }

    private fun getProductForCart(productId: Long): ProductEntity {
        val product = productRepository.findById(productId).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(404), "Продукт не найден")
        }

        if (!product.isActive) {
            throw ResponseStatusException(HttpStatusCode.valueOf(400), "Продукт недоступен")
        }

        if (!product.category.isActive) {
            throw ResponseStatusException(HttpStatusCode.valueOf(400), "Категория продукта недоступна")
        }

        return product
    }

    private fun validateQuantityStep(quantity: Int, countStep: Int) {
        if (countStep <= 0 || quantity % countStep != 0) {
            throw ResponseStatusException(HttpStatusCode.valueOf(400), "Некорректный шаг количества")
        }
    }

    @Transactional
    fun updateOrderItemQuantity(deviceId: String, cartItemId: Long, quantity: Int): CartDto {
        val cart = cartRepository.findByDeviceId(deviceId)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Cart not found")

        val existingItem = cart.items.find { it.id == cartItemId }
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(404), "Cart item not found")

        if (quantity == 0) {
            cart.removeItem { existingItem }
        } else {
            existingItem.quantity = quantity
        }

        cart.updateTotalPrice()

        val savedCart = cartRepository.save(cart)
        return cartMapper.toDto(savedCart)
    }

    @Transactional
    fun removeAll(deviceId: String): CartDto? {
        val cart = cartRepository.findByDeviceId(deviceId) ?: return null
        cart.items.clear()
        cart.totalPrice = BigDecimal.ZERO
        val savedCart = cartRepository.save(cart)
        return cartMapper.toDto(savedCart)
    }

    fun getCart(deviceId: String): CartDto? {
        val cartEntity = cartRepository.findByDeviceId(deviceId) ?: return null

        val deliveryInfo = deliveryPriceCalculator.calculateDeliveryPrice(
            cartEntity.deliveryType,
            cartEntity.deliveryAddress?.latitude,
            cartEntity.deliveryAddress?.longitude,
            cartEntity.deliveryAddress?.cityEntity?.id,
            departmentMapper.toDto(cartEntity.department)
        )

        when (cartEntity.deliveryType) {
            DeliveryType.PICKUP -> {
                cartEntity.deliveryAddress = null
                cartEntity.deliveryPrice = BigDecimal.ZERO
                cartEntity.freeDeliveryPrice = BigDecimal.ZERO
            }
            DeliveryType.DELIVERY -> {

                cartEntity.deliveryPrice = BigDecimal(deliveryInfo?.deliveryPrice ?: 250)
                cartEntity.freeDeliveryPrice = deliveryInfo?.freeDeliveryPrice?.let { BigDecimal(it) }
            }
        }

        val cartProductIds = cartEntity.items.map { it.product.id!! }

        val products = productRepository.findAllById(cartProductIds)

        val cartItems = cartEntity.items.mapNotNull { cartItem ->
            val productEntity = products.firstOrNull { it.id == cartItem.product.id!! } ?: return@mapNotNull null
            cartItem.apply {
                product = productEntity
            }
        }.toMutableSet()

        cartEntity.setItems { cartItems }
        cartEntity.updateTotalPrice()

        val updatedCartEntity = cartRepository.save(cartEntity)
        return cartMapper.toDto(updatedCartEntity)
    }

    @Transactional
    fun updateDeliveryAddress(
        deviceId: String,
        deliveryType: DeliveryType,
        deliveryAddress: AddressDto?,
        departmentId: Long,
        deliveryInfo: DeliveryInfo,
        comment: String?,
    ): CartDto? {
        val cart = cartRepository.findByDeviceId(deviceId) ?: return null
        val department = departmentRepository.findById(departmentId).getOrNull() ?: return null
        val newAddress = if (deliveryType == DeliveryType.DELIVERY && deliveryAddress != null) {
            val cartAddress = cart.deliveryAddress
            val city = cityRepository.findById(deliveryAddress.city.id).getOrNull() ?: return null

            if (cartAddress == null) {
                AddressEntity(
                    cityEntity = city,
                    street = deliveryAddress.street,
                    house = deliveryAddress.house,
                    entrance = deliveryAddress.entrance,
                    flat = deliveryAddress.flat,
                    intercome = deliveryAddress.intercome,
                    comment = deliveryAddress.comment,
                    latitude = deliveryAddress.latitude,
                    longitude = deliveryAddress.longitude
                )
            } else {
                cartAddress.cityEntity = city
                cartAddress.street = deliveryAddress.street
                cartAddress.house = deliveryAddress.house
                cartAddress.entrance = deliveryAddress.entrance
                cartAddress.flat = deliveryAddress.flat
                cartAddress.intercome = deliveryAddress.intercome
                cartAddress.comment = deliveryAddress.comment
                cartAddress
            }
        } else null

        cart.deliveryType = deliveryType
        cart.department = department
        cart.deliveryPrice = BigDecimal(deliveryInfo.deliveryPrice)
        cart.freeDeliveryPrice = deliveryInfo.freeDeliveryPrice?.let {
            BigDecimal(it)
        }
        cart.deliveryAddress = newAddress
        cart.comment = comment
        cart.updateTotalPrice()

        val savedCart = cartRepository.save(cart)
        return cartMapper.toDto(savedCart)
    }
}
