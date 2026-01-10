package ru.foodbox.delivery.services.broadcast

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class AuthBroadcaster {
    private val log = LoggerFactory.getLogger(AuthBroadcaster::class.java)

    private val subscriptions: ConcurrentHashMap<String, MutableSet<WebSocketSession>> = ConcurrentHashMap()

    private val mapper = jacksonObjectMapper()

    fun subscribe(checkId: String, session: WebSocketSession) {
        subscriptions.compute(checkId) { _, existing ->
            val set = existing ?: ConcurrentHashMap.newKeySet()
            set.add(session)
            set
        }
    }

    fun unsubscribe(checkId: String, session: WebSocketSession) {
        subscriptions[checkId]?.let { set ->
            set.remove(session)
            if (set.isEmpty()) {
                subscriptions.remove(checkId)
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

    fun broadcastUpdate(checkId: String) {
        val subscribers = subscriptions[checkId]?.toList().orEmpty()

        if (subscribers.isEmpty()) {
            log.info("No subscribers for checkId $checkId, skipping broadcast")
            return
        }

        val message = TextMessage("confirmed")

        log.info("Broadcasting update for checkId $checkId to ${subscribers.size} sessions")

        subscribers.forEach { session ->
            try {
                if (session.isOpen) {
                    session.sendMessage(message)
                } else {
                    unsubscribe(checkId, session)
                }
            } catch (e: Exception) {
                log.warn("Failed to send update to session ${session.id}: ${e.message}")
                unsubscribe(checkId, session)
                try { session.close() } catch (_: Exception) {}
            }
        }
    }
}