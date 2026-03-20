package ru.foodbox.delivery.modules.checkout.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentMethodRule
import ru.foodbox.delivery.modules.checkout.domain.repository.CheckoutPaymentMethodRuleRepository
import ru.foodbox.delivery.modules.delivery.application.DeliveryService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import ru.foodbox.delivery.modules.payments.application.PaymentService
import ru.foodbox.delivery.modules.payments.application.command.CreatePaymentCommand
import ru.foodbox.delivery.modules.payments.domain.Payment
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CheckoutServiceImplTest {

    @Test
    fun `returns only delivery options with active configured payment methods`() {
        val service = CheckoutServiceImpl(
            deliveryService = StubDeliveryService(
                methods = listOf(
                    DeliveryMethodType.PICKUP,
                    DeliveryMethodType.COURIER,
                    DeliveryMethodType.YANDEX_PICKUP_POINT,
                ),
            ),
            paymentService = StubPaymentService(
                methods = listOf(
                    paymentMethodInfo(PaymentMethodCode.CASH, isActive = true),
                    paymentMethodInfo(PaymentMethodCode.CARD_ON_DELIVERY, isActive = true),
                    paymentMethodInfo(PaymentMethodCode.CARD_ONLINE, isActive = false),
                ),
            ),
            checkoutPaymentMethodRuleRepository = StubCheckoutPaymentMethodRuleRepository(
                listOf(
                    CheckoutPaymentMethodRule(
                        deliveryMethod = DeliveryMethodType.PICKUP,
                        paymentMethods = listOf(
                            PaymentMethodCode.CASH,
                            PaymentMethodCode.CARD_ON_DELIVERY,
                        ),
                    ),
                    CheckoutPaymentMethodRule(
                        deliveryMethod = DeliveryMethodType.COURIER,
                        paymentMethods = listOf(
                            PaymentMethodCode.CARD_ONLINE,
                            PaymentMethodCode.CASH,
                        ),
                    ),
                    CheckoutPaymentMethodRule(
                        deliveryMethod = DeliveryMethodType.YANDEX_PICKUP_POINT,
                        paymentMethods = listOf(PaymentMethodCode.SBP),
                    ),
                )
            ),
        )

        val options = service.getAvailableOptions()

        assertEquals(
            listOf(DeliveryMethodType.PICKUP, DeliveryMethodType.COURIER),
            options.map { it.deliveryMethod },
        )
        assertEquals(
            listOf(PaymentMethodCode.CASH, PaymentMethodCode.CARD_ON_DELIVERY),
            options[0].paymentMethods.map { it.code },
        )
        assertEquals(
            listOf(PaymentMethodCode.CASH),
            options[1].paymentMethods.map { it.code },
        )
    }

    @Test
    fun `fails fast when available delivery method has no checkout payment rule`() {
        val service = CheckoutServiceImpl(
            deliveryService = StubDeliveryService(
                methods = listOf(DeliveryMethodType.COURIER),
            ),
            paymentService = StubPaymentService(
                methods = listOf(paymentMethodInfo(PaymentMethodCode.CASH, isActive = true)),
            ),
            checkoutPaymentMethodRuleRepository = StubCheckoutPaymentMethodRuleRepository(emptyList()),
        )

        assertFailsWith<IllegalStateException> {
            service.getAvailableOptions()
        }
    }

    private fun paymentMethodInfo(
        code: PaymentMethodCode,
        isActive: Boolean,
    ): PaymentMethodInfo {
        return PaymentMethodInfo(
            code = code,
            name = code.displayName,
            description = code.description,
            isOnline = code.isOnline,
            isActive = isActive,
        )
    }

    private class StubCheckoutPaymentMethodRuleRepository(
        private val rules: List<CheckoutPaymentMethodRule>,
    ) : CheckoutPaymentMethodRuleRepository {
        override fun findAll(): List<CheckoutPaymentMethodRule> = rules
    }

    private class StubDeliveryService(
        private val methods: List<DeliveryMethodType>,
    ) : DeliveryService {
        override fun getAvailableMethods(): List<DeliveryMethodType> = methods

        override fun getActivePickupPoints(): List<PickupPoint> = emptyList()

        override fun detectYandexLocations(query: String): List<YandexDeliveryLocationVariant> = emptyList()

        override fun getYandexPickupPoints(geoId: Long): List<YandexPickupPointOption> = emptyList()

        override fun calculateQuote(context: DeliveryQuoteContext): DeliveryQuote {
            throw UnsupportedOperationException("Not used in checkout tests")
        }
    }

    private class StubPaymentService(
        private val methods: List<PaymentMethodInfo>,
    ) : PaymentService {
        override fun getAvailableMethods(): List<PaymentMethodInfo> = methods

        override fun createPayment(actor: CurrentActor, command: CreatePaymentCommand): Payment {
            throw UnsupportedOperationException("Not used in checkout tests")
        }

        override fun getPayment(actor: CurrentActor, paymentId: UUID): Payment {
            throw UnsupportedOperationException("Not used in checkout tests")
        }
    }
}
