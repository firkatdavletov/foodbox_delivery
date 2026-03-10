package ru.foodbox.delivery.controllers.websockets

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
    private val broadcaster: AuthBroadcaster,
    private val authService: AuthService,
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(AuthWebSocketHandler::class.java)
    private val mapper = jacksonObjectMapper()
    private val sessions = ConcurrentHashMap.newKeySet<WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
        log.info { "WS connected: ${session.id}" }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload.trim()
        log.debug { "WS message from ${session.id}: $payload" }
        when {
            payload.startsWith("subscribe") -> {
                val checkId = session.attributes["check_id"] as? String
                if (checkId != null) {
                    broadcaster.subscribe(checkId, session)
                    val tokenPair = authService.checkConfirmation(checkId)

                    if (tokenPair != null) {
                        val message = mapper.writeValueAsString(tokenPair)
                        session.sendMessage(TextMessage(message))
                    } else {
                        session.sendMessage(PongMessage())
                    }
                    log.info {"Session ${session.id} subscribed to check_id $checkId" }
                }
            }
            payload.startsWith("unsubscribe") -> {
                val checkId = session.attributes["check_id"] as? String ?: return
                broadcaster.unsubscribe(checkId, session)
                session.sendMessage(TextMessage("unsubscribed"))
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