package ru.foodbox.delivery.services.broadcast

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import ru.foodbox.delivery.services.dto.TokenPairDto
import java.util.concurrent.ConcurrentHashMap

@Service
class AuthBroadcaster {
    private val logger = LoggerFactory.getLogger(AuthBroadcaster::class.java)

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
            }
        }
    }

    fun broadcastUpdate(checkId: String, tokenPairDto: TokenPairDto) {
        val subscribers = subscriptions[checkId]?.toList().orEmpty()

        if (subscribers.isEmpty()) {
            return
        }

        val payload = mapper.writeValueAsString(tokenPairDto)
        val message = TextMessage(payload)

        subscribers.forEach { session ->
            try {
                if (session.isOpen) {
                    session.sendMessage(message)
                } else {
                    unsubscribe(checkId, session)
                }
            } catch (e: Exception) {
                unsubscribe(checkId, session)
                try { session.close() } catch (_: Exception) {}
            }
        }
    }
}