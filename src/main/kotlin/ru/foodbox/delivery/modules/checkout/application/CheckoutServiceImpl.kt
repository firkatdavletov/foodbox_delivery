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

    override fun getAvailableOptions(): List<CheckoutDeliveryOption> {
        val availablePaymentMethods = paymentService.getAvailableMethods()
            .filter { it.isActive }
            .associateBy { it.code }
        val rulesByDeliveryMethod = checkoutPaymentMethodRuleRepository.findAll()
            .associateBy { it.deliveryMethod }

        return deliveryService.getAvailableMethods().mapNotNull { deliveryMethod ->
            val paymentMethods = resolvePaymentMethods(
                deliveryMethod = deliveryMethod,
                availablePaymentMethods = availablePaymentMethods,
                rulesByDeliveryMethod = rulesByDeliveryMethod,
            )
            if (paymentMethods.isEmpty()) {
                logger.info(
                    "Checkout delivery method {} is skipped because no configured payment methods are currently available",
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

    private fun resolvePaymentMethods(
        deliveryMethod: DeliveryMethodType,
        availablePaymentMethods: Map<PaymentMethodCode, PaymentMethodInfo>,
        rulesByDeliveryMethod: Map<DeliveryMethodType, CheckoutPaymentMethodRule>,
    ): List<PaymentMethodInfo> {
        val rule = rulesByDeliveryMethod[deliveryMethod]
            ?: throw IllegalStateException("Checkout payment rule is not configured for delivery method $deliveryMethod")

        return rule.paymentMethods.mapNotNull(availablePaymentMethods::get)
    }
}
