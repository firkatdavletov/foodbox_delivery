package ru.foodbox.delivery.controllers.websockets

import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.PongMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import ru.foodbox.delivery.services.broadcast.AuthBroadcaster
import java.util.concurrent.ConcurrentHashMap

@Component
class AuthWebSocketHandler(
    private val broadcaster: AuthBroadcaster
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(AuthWebSocketHandler::class.java)
    private val sessions = ConcurrentHashMap.newKeySet<WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
        log.info { "WS connected: ${session.id}" }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload.trim()
        log.debug { "WS message from ${session.id}: $payload" }

        // simple protocol:
        // subscribe:ORDER_ID
        // unsubscribe:ORDER_ID
        when {
            payload.startsWith("subscribe") -> {
                val checkId = session.attributes["check_id"] as? String ?: return
                broadcaster.subscribe(checkId, session)
                session.sendMessage(PongMessage())
                log.info {"Session ${session.id} subscribed to check_id $checkId" }
            }
            payload.startsWith("unsubscribe") -> {
                val checkId = session.attributes["check_id"] as? String ?: return
                broadcaster.unsubscribe(checkId, session)
                session.sendMessage(PongMessage())
                log.info {"Session ${session.id} unsubscribed from $checkId" }
            }
            else -> {
                session.sendMessage(TextMessage("unknown-command"))
            }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
        broadcaster.removeSessionFromAll(session)
        log.info {"WS disconnected: ${session.id} (${status.code})" }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.warn { "Transport error for session ${session.id}: ${exception.message}" }
        try {
            session.close(CloseStatus.SERVER_ERROR)
        } catch (e: Exception) {
            log.warn { "Error closing session: ${e.message}" }
        }
    }
}