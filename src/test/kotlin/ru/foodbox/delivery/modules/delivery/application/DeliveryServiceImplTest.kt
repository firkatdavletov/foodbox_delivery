package ru.foodbox.delivery.modules.delivery.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryMethodSettingRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository
import ru.foodbox.delivery.modules.payments.application.PaymentService
import ru.foodbox.delivery.modules.payments.application.command.CreatePaymentCommand
import ru.foodbox.delivery.modules.payments.domain.Payment
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo
import java.util.UUID
import kotlin.test.assertEquals

class DeliveryServiceImplTest {

    @Test
    fun `returns enabled local methods when yandex delivery is not configured`() {
        val yandexDeliveryGateway = RecordingYandexDeliveryGateway(isConfigured = false)
        val service = DeliveryServiceImpl(
            deliveryMethodSettingRepository = StubDeliveryMethodSettingRepository(allEnabledMethodSettings()),
            pickupPointRepository = StubPickupPointRepository(),
            yandexDeliveryGateway = yandexDeliveryGateway,
            paymentService = StubPaymentService(methods = emptyList()),
            calculators = emptyList(),
        )

        val result = service.getAvailableMethods()

        assertEquals(listOf(DeliveryMethodType.PICKUP, DeliveryMethodType.COURIER), result)
    }

    @Test
    fun `returns all enabled methods when yandex delivery is configured`() {
        val yandexDeliveryGateway = RecordingYandexDeliveryGateway(isConfigured = true)
        val service = DeliveryServiceImpl(
            deliveryMethodSettingRepository = StubDeliveryMethodSettingRepository(allEnabledMethodSettings()),
            pickupPointRepository = StubPickupPointRepository(),
            yandexDeliveryGateway = yandexDeliveryGateway,
            paymentService = StubPaymentService(methods = emptyList()),
            calculators = emptyList(),
        )

        val result = service.getAvailableMethods()

        assertEquals(
            listOf(
                DeliveryMethodType.PICKUP,
                DeliveryMethodType.COURIER,
                DeliveryMethodType.YANDEX_PICKUP_POINT,
            ),
            result,
        )
    }

    @Test
    fun `uses already paid filter for yandex pickup points when online payment is available`() {
        val yandexDeliveryGateway = RecordingYandexDeliveryGateway()
        val service = DeliveryServiceImpl(
            deliveryMethodSettingRepository = StubDeliveryMethodSettingRepository(allEnabledMethodSettings()),
            pickupPointRepository = StubPickupPointRepository(),
            yandexDeliveryGateway = yandexDeliveryGateway,
            paymentService = StubPaymentService(
                methods = listOf(
                    paymentMethodInfo(PaymentMethodCode.CASH, isActive = true),
                    paymentMethodInfo(PaymentMethodCode.CARD_ONLINE, isActive = true),
                ),
            ),
            calculators = emptyList(),
        )

        val result = service.getYandexPickupPoints(213L)

        assertEquals(yandexDeliveryGateway.pickupPoints, result)
        assertEquals(213L, yandexDeliveryGateway.lastGeoId)
        assertEquals("already_paid", yandexDeliveryGateway.lastPaymentMethod)
    }

    @Test
    fun `uses card on receipt filter for yandex pickup points when no active online payment is available`() {
        val yandexDeliveryGateway = RecordingYandexDeliveryGateway()
        val service = DeliveryServiceImpl(
            deliveryMethodSettingRepository = StubDeliveryMethodSettingRepository(allEnabledMethodSettings()),
            pickupPointRepository = StubPickupPointRepository(),
            yandexDeliveryGateway = yandexDeliveryGateway,
            paymentService = StubPaymentService(
                methods = listOf(
                    paymentMethodInfo(PaymentMethodCode.CASH, isActive = true),
                    paymentMethodInfo(PaymentMethodCode.CARD_ONLINE, isActive = false),
                ),
            ),
            calculators = emptyList(),
        )

        val result = service.getYandexPickupPoints(54L)

        assertEquals(yandexDeliveryGateway.pickupPoints, result)
        assertEquals(54L, yandexDeliveryGateway.lastGeoId)
        assertEquals("card_on_receipt", yandexDeliveryGateway.lastPaymentMethod)
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

    private fun allEnabledMethodSettings(): List<DeliveryMethodSetting> {
        return DeliveryMethodType.entries.mapIndexed { index, method ->
            DeliveryMethodSetting(
                method = method,
                enabled = true,
                sortOrder = index,
            )
        }
    }

    private class RecordingYandexDeliveryGateway(
        private val isConfigured: Boolean,
    ) : YandexDeliveryGateway {
        val pickupPoints = listOf(
            YandexPickupPointOption(
                id = "yandex-1",
                name = "Yandex Point 1",
                address = "Address 1",
            )
        )
        var lastGeoId: Long? = null
        var lastPaymentMethod: String? = null

        constructor() : this(isConfigured = true)

        override fun isConfigured(): Boolean = isConfigured

        override fun detectLocations(query: String): List<YandexDeliveryLocationVariant> = emptyList()

        override fun listPickupPoints(geoId: Long, paymentMethod: String?): List<YandexPickupPointOption> {
            lastGeoId = geoId
            lastPaymentMethod = paymentMethod
            return pickupPoints
        }

        override fun getPickupPoint(pickupPointId: String): YandexPickupPointOption? = null

        override fun calculateSelfPickupPrice(
            pickupPointId: String,
            subtotalMinor: Long,
            totalWeightGrams: Long?,
        ): YandexDeliveryPricingQuote {
            throw UnsupportedOperationException("Not used in delivery service tests")
        }

        override fun createOffers(request: YandexOfferCreateRequest): List<YandexDeliveryOffer> {
            throw UnsupportedOperationException("Not used in delivery service tests")
        }

        override fun confirmOffer(offerId: String): YandexConfirmedDeliveryRequest {
            throw UnsupportedOperationException("Not used in delivery service tests")
        }
    }

    private class StubDeliveryMethodSettingRepository(
        private val settings: List<DeliveryMethodSetting>,
    ) : DeliveryMethodSettingRepository {
        override fun findAll(): List<DeliveryMethodSetting> = settings

        override fun findByMethod(method: DeliveryMethodType): DeliveryMethodSetting? {
            return settings.firstOrNull { it.method == method }
        }

        override fun save(setting: DeliveryMethodSetting): DeliveryMethodSetting = setting
    }

    private class StubPickupPointRepository : PickupPointRepository {
        override fun findAll(): List<PickupPoint> = emptyList()

        override fun findAllByIsActive(isActive: Boolean): List<PickupPoint> = emptyList()

        override fun findById(id: UUID): PickupPoint? = null

        override fun findByCode(code: String): PickupPoint? = null

        override fun save(point: PickupPoint): PickupPoint = point

        override fun findActiveById(id: UUID): PickupPoint? = null

        override fun findAllActive(): List<PickupPoint> = emptyList()
    }

    private class StubPaymentService(
        private val methods: List<PaymentMethodInfo>,
    ) : PaymentService {
        override fun getAvailableMethods(): List<PaymentMethodInfo> = methods

        override fun createPayment(actor: CurrentActor, command: CreatePaymentCommand): Payment {
            throw UnsupportedOperationException("Not used in delivery service tests")
        }

        override fun getPayment(actor: CurrentActor, paymentId: UUID): Payment {
            throw UnsupportedOperationException("Not used in delivery service tests")
        }
    }
}
