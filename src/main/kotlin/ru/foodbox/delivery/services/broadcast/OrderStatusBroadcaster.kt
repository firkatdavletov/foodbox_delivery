package ru.foodbox.delivery.services.broadcast

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import ru.foodbox.delivery.controllers.websockets.model.OrderStatusUpdate
import ru.foodbox.delivery.data.entities.OrderStatus
import java.util.concurrent.ConcurrentHashMap

@Service
class OrderStatusBroadcaster {
    private val log = LoggerFactory.getLogger(OrderStatusBroadcaster::class.java)
    // map orderId -> set of sessions
    private val subscriptions: ConcurrentHashMap<String, MutableSet<WebSocketSession>> = ConcurrentHashMap()
    private val mapper = jacksonObjectMapper()

    fun subscribe(userId: String, session: WebSocketSession) {
        subscriptions.compute(userId) { _, existing ->
            val set = existing ?: ConcurrentHashMap.newKeySet()
            set.add(session)
            set
        }
    }

    fun unsubscribe(userId: String, session: WebSocketSession) {
        subscriptions[userId]?.let { set ->
            set.remove(session)
            if (set.isEmpty()) {
                subscriptions.remove(userId)
            }
        }
    }

    fun removeSessionFromAll(session: WebSocketSession) {
        subscriptions.forEach { (userId, set) ->
            if (set.remove(session)) {
                if (set.isEmpty()) subscriptions.remove(userId)
                log.debug("Removed session ${session.id} from $userId")
            }
        }
    }

    /**
     * Call this when order status changes.
     */
    fun broadcastUpdate(userId: Long, orderId: Long, status: OrderStatus) {
        val subscribers = subscriptions[userId.toString()]?.toList().orEmpty()

        if (subscribers.isEmpty()) {
            log.info("No subscribers for order $userId, skipping broadcast")
            return
        }

        val updateData = OrderStatusUpdate(orderId, status)

        val payload = mapper.writeValueAsString(updateData)
        val message = TextMessage(payload)

        log.info("Broadcasting update for order $userId to ${subscribers.size} sessions")

        subscribers.forEach { session ->
            try {
                if (session.isOpen) {
                    session.sendMessage(message)
                } else {
                    unsubscribe(userId.toString(), session)
                }
            } catch (e: Exception) {
                log.warn("Failed to send update to session ${session.id}: ${e.message}")
                unsubscribe(userId.toString(), session)
                try { session.close() } catch (_: Exception) {}
            }
        }
    }
}