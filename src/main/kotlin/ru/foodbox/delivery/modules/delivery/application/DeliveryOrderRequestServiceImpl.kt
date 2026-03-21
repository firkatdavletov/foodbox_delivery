package ru.foodbox.delivery.modules.delivery.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.delivery.domain.DeliveryOffer
import ru.foodbox.delivery.modules.delivery.domain.DeliveryOfferProvider
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryValidationException
import ru.foodbox.delivery.modules.delivery.domain.OrderDeliveryOffer
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryOfferRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.OrderDeliveryOfferRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.yandex.YandexDeliveryProperties
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class DeliveryOrderRequestServiceImpl(
    private val yandexDeliveryGateway: YandexDeliveryGateway,
    private val yandexDeliveryProperties: YandexDeliveryProperties,
    private val deliveryOfferRepository: DeliveryOfferRepository,
    private val orderDeliveryOfferRepository: OrderDeliveryOfferRepository,
) : DeliveryOrderRequestService {

    @Transactional
    override fun createAndConfirm(order: Order): DeliveryOrderRequestConfirmation? {
        if (order.delivery.method != DeliveryMethodType.YANDEX_PICKUP_POINT) {
            return null
        }
        if (!yandexDeliveryGateway.isConfigured()) {
            throw IllegalStateException("Yandex delivery integration is not configured")
        }
        if (orderDeliveryOfferRepository.findByOrderId(order.id) != null) {
            throw IllegalStateException("Delivery offer is already linked to order ${order.id}")
        }

        val paymentMethodCode = order.payment?.methodCode
            ?: throw DeliveryValidationException("Order payment method is not selected")
        val pickupPointExternalId = order.delivery.pickupPointExternalId?.trim()?.takeIf { it.isNotBlank() }
            ?: throw DeliveryValidationException("Yandex pickup point is not selected")
        val recipientPhone = order.customerPhone?.trim()?.takeIf { it.isNotBlank() }
            ?: throw DeliveryValidationException("Order customer phone is required")
        val yandexPaymentMethod = resolvePaymentMethod(paymentMethodCode)
        val now = Instant.now()

        val selectedOffer = selectOffer(
            yandexDeliveryGateway.createOffers(
                YandexOfferCreateRequest(
                    operatorRequestId = order.id.toString(),
                    comment = order.comment,
                    destinationPickupPointId = pickupPointExternalId,
                    recipientName = order.customerName,
                    recipientPhone = recipientPhone,
                    recipientEmail = order.customerEmail,
                    items = order.items.map { item ->
                        YandexOfferItem(
                            id = item.id.toString(),
                            name = item.title,
                            article = (item.variantId ?: item.productId).toString(),
                            count = item.quantity,
                            unitPriceMinor = item.priceMinor,
                        )
                    },
                    paymentMethod = yandexPaymentMethod,
                    deliveryCostMinor = if (paymentMethodCode.isOnline) 0L else order.deliveryFeeMinor,
                    pickupIntervalFrom = now,
                    pickupIntervalTo = now.plus(yandexDeliveryProperties.sourcePickupIntervalHours, ChronoUnit.HOURS),
                )
            )
        )

        val savedOffer = deliveryOfferRepository.save(
            DeliveryOffer(
                id = UUID.randomUUID(),
                provider = DeliveryOfferProvider.YANDEX,
                externalOfferId = selectedOffer.externalOfferId,
                expiresAt = selectedOffer.expiresAt,
                pricingMinor = selectedOffer.pricingMinor,
                pricingTotalMinor = selectedOffer.pricingTotalMinor,
                currency = selectedOffer.currency,
                commissionOnDeliveryPercent = selectedOffer.commissionOnDeliveryPercent,
                commissionOnDeliveryAmountMinor = selectedOffer.commissionOnDeliveryAmountMinor,
                deliveryPolicy = selectedOffer.deliveryPolicy,
                deliveryIntervalFrom = selectedOffer.deliveryIntervalFrom,
                deliveryIntervalTo = selectedOffer.deliveryIntervalTo,
                pickupIntervalFrom = selectedOffer.pickupIntervalFrom,
                pickupIntervalTo = selectedOffer.pickupIntervalTo,
                createdAt = now,
                updatedAt = now,
            )
        )

        val savedLink = orderDeliveryOfferRepository.save(
            OrderDeliveryOffer(
                id = UUID.randomUUID(),
                orderId = order.id,
                offerId = savedOffer.id,
                externalRequestId = null,
                createdAt = now,
                updatedAt = now,
                confirmedAt = null,
            )
        )

        val confirmed = yandexDeliveryGateway.confirmOffer(savedOffer.externalOfferId)
        orderDeliveryOfferRepository.save(savedLink.confirm(confirmed.externalRequestId, Instant.now()))

        val deliveryFeeMinor = selectedOffer.pricingTotalMinor
            ?: selectedOffer.pricingMinor
            ?: throw IllegalStateException("Yandex delivery offer does not contain pricing")
        val currency = selectedOffer.currency
            ?: order.delivery.currency

        return DeliveryOrderRequestConfirmation(
            externalOfferId = savedOffer.externalOfferId,
            externalRequestId = confirmed.externalRequestId,
            deliveryFeeMinor = deliveryFeeMinor,
            currency = currency,
        )
    }

    private fun selectOffer(offers: List<YandexDeliveryOffer>): YandexDeliveryOffer {
        return offers.minWithOrNull(
            compareBy<YandexDeliveryOffer>(
                { it.pricingTotalMinor ?: it.pricingMinor ?: Long.MAX_VALUE },
                { it.expiresAt ?: Instant.MAX },
            )
        ) ?: throw IllegalStateException("Yandex Delivery API returned no offers")
    }

    private fun resolvePaymentMethod(paymentMethodCode: PaymentMethodCode): YandexOfferPaymentMethod {
        return when {
            paymentMethodCode.isOnline -> YandexOfferPaymentMethod.ALREADY_PAID
            paymentMethodCode == PaymentMethodCode.CARD_ON_DELIVERY -> YandexOfferPaymentMethod.CARD_ON_RECEIPT
            else -> throw DeliveryValidationException(
                "Yandex pickup point does not support selected payment method ${paymentMethodCode.name}",
            )
        }
    }
}
