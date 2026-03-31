package ru.foodbox.delivery.modules.checkout.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.checkout.domain.CheckoutDeliveryOption
import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentMethodRule
import ru.foodbox.delivery.modules.checkout.domain.repository.CheckoutPaymentMethodRuleRepository
import ru.foodbox.delivery.modules.delivery.application.DeliveryService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.payments.application.PaymentService
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo

@Service
class CheckoutServiceImpl(
    private val deliveryService: DeliveryService,
    private val paymentService: PaymentService,
    private val checkoutPaymentMethodRuleRepository: CheckoutPaymentMethodRuleRepository,
) : CheckoutService {

    private val logger = LoggerFactory.getLogger(CheckoutServiceImpl::class.java)

    override fun getAvailableOptions(query: CheckoutOptionsQuery): List<CheckoutDeliveryOption> {
        val availablePaymentMethods = paymentService.getAvailableMethods()
            .filter { it.isActive }
            .associateBy { it.code }
        val rulesByDeliveryMethod = checkoutPaymentMethodRuleRepository.findAll()
            .associateBy { it.deliveryMethod }

        return deliveryService.getAvailableMethods().mapNotNull { deliveryMethod ->
            val paymentMethods = when (deliveryMethod) {
                DeliveryMethodType.YANDEX_PICKUP_POINT -> resolveYandexPaymentMethods(
                    pickupPointId = query.pickupPointId,
                    availablePaymentMethods = availablePaymentMethods,
                )
                else -> resolveConfiguredPaymentMethods(
                    deliveryMethod = deliveryMethod,
                    availablePaymentMethods = availablePaymentMethods,
                    rulesByDeliveryMethod = rulesByDeliveryMethod,
                )
            }
            if (paymentMethods.isEmpty()) {
                logger.info(
                    "Checkout delivery method {} is skipped because no available payment methods remain after resolution",
                    deliveryMethod,
                )
                null
            } else {
                CheckoutDeliveryOption(
                    deliveryMethod = deliveryMethod,
                    paymentMethods = paymentMethods,
                )
            }
        }
    }

    private fun resolveConfiguredPaymentMethods(
        deliveryMethod: DeliveryMethodType,
        availablePaymentMethods: Map<PaymentMethodCode, PaymentMethodInfo>,
        rulesByDeliveryMethod: Map<DeliveryMethodType, CheckoutPaymentMethodRule>,
    ): List<PaymentMethodInfo> {
        val rule = rulesByDeliveryMethod[deliveryMethod]
            ?: return emptyList()

        return rule.paymentMethods.mapNotNull(availablePaymentMethods::get)
    }

    private fun resolveYandexPaymentMethods(
        pickupPointId: String?,
        availablePaymentMethods: Map<PaymentMethodCode, PaymentMethodInfo>,
    ): List<PaymentMethodInfo> {
        val normalizedPickupPointId = pickupPointId?.trim()?.takeIf { it.isNotBlank() } ?: run {
            logger.debug("Checkout Yandex pickup point payment methods skipped because pickupPointId is not provided")
            return emptyList()
        }

        return try {
            val resolvedCodes = linkedSetOf<PaymentMethodCode>()
            deliveryService.getYandexPickupPoint(normalizedPickupPointId)
                ?.paymentMethods
                .orEmpty()
                .map { it.trim().lowercase() }
                .forEach { paymentMethod ->
                    when (paymentMethod) {
                        YANDEX_PAYMENT_METHOD_ALREADY_PAID -> {
                            availablePaymentMethods.values
                                .filter { it.isOnline }
                                .mapTo(resolvedCodes) { it.code }
                        }
                        YANDEX_PAYMENT_METHOD_CARD_ON_RECEIPT,
                        YANDEX_PAYMENT_METHOD_POSTPAY,
                        -> availablePaymentMethods[PaymentMethodCode.CARD_ON_DELIVERY]
                            ?.code
                            ?.let(resolvedCodes::add)
                        YANDEX_PAYMENT_METHOD_CASH_ON_RECEIPT -> logger.debug(
                            "Yandex pickup point payment method {} is ignored because offers/create supports only already_paid, card_on_receipt and postpay",
                            paymentMethod,
                        )
                        else -> logger.debug(
                            "Unsupported Yandex pickup point payment method {} returned for pickupPointId={}",
                            paymentMethod,
                            normalizedPickupPointId,
                        )
                    }
                }

            resolvedCodes
                .sortedBy { it.ordinal }
                .mapNotNull(availablePaymentMethods::get)
        } catch (ex: Exception) {
            logger.warn(
                "Checkout Yandex pickup point payment methods are unavailable for pickupPointId={} errorType={}",
                normalizedPickupPointId,
                ex.javaClass.simpleName,
            )
            emptyList()
        }
    }

    private companion object {
        private const val YANDEX_PAYMENT_METHOD_ALREADY_PAID = "already_paid"
        private const val YANDEX_PAYMENT_METHOD_CARD_ON_RECEIPT = "card_on_receipt"
        private const val YANDEX_PAYMENT_METHOD_CASH_ON_RECEIPT = "cash_on_receipt"
        private const val YANDEX_PAYMENT_METHOD_POSTPAY = "postpay"
    }
}
